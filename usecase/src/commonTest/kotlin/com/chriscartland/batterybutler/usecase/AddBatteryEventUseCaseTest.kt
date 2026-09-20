package com.chriscartland.batterybutler.usecase

import com.chriscartland.batterybutler.testcommon.FakeDeviceRepository
import com.chriscartland.batterybutler.testcommon.FakeNeedsBatteryRepository
import com.chriscartland.batterybutler.testcommon.TestDevices
import com.chriscartland.batterybutler.usecase.SetDeviceNeedsBatteryUseCase
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.test.runTest
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertTrue
import kotlin.time.ExperimentalTime
import kotlin.time.Instant

@OptIn(ExperimentalTime::class)
class AddBatteryEventUseCaseTest {
    @Test
    fun `invoke adds event to repository`() =
        runTest {
            val repo = FakeDeviceRepository()
            val updateLastReplaced = UpdateDeviceLastReplacedUseCase(repo)
            val useCase = AddBatteryEventUseCase(repo, updateLastReplaced, SetDeviceNeedsBatteryUseCase(FakeNeedsBatteryRepository()))
            val device = TestDevices.createDevice(id = "d1", name = "Test Device")
            repo.setDevices(listOf(device))
            val event = TestDevices.createBatteryEvent(id = "e1", deviceId = "d1")

            useCase(event)

            assertTrue(repo.events.contains(event))
        }

    @Test
    fun `invoke updates device lastReplaced timestamp`() =
        runTest {
            val repo = FakeDeviceRepository()
            val updateLastReplaced = UpdateDeviceLastReplacedUseCase(repo)
            val useCase = AddBatteryEventUseCase(repo, updateLastReplaced, SetDeviceNeedsBatteryUseCase(FakeNeedsBatteryRepository()))
            val device = TestDevices.createDevice(
                id = "d1",
                name = "Smoke Detector",
                batteryLastReplaced = Instant.DISTANT_PAST,
            )
            repo.setDevices(listOf(device))
            val replacementDate = Instant.parse("2024-06-15T10:30:00Z")
            val event = TestDevices.createBatteryEvent(
                id = "e1",
                deviceId = "d1",
                date = replacementDate,
            )

            useCase(event)

            val updatedDevice = repo.getAllDevices().first().first { it.id == "d1" }
            assertEquals(replacementDate, updatedDevice.batteryLastReplaced)
        }

    /**
     * The rule that keeps the "needs a new battery" mark honest: a mark is a to-do, so recording
     * the replacement has to tick it off. Without this the device would still read "Needs battery"
     * immediately after the user logged changing it.
     */
    @Test
    fun `invoke clears the device's needs-battery mark`() =
        runTest {
            val repo = FakeDeviceRepository()
            val needsBattery = FakeNeedsBatteryRepository(initialFlagged = setOf("d1"))
            repo.setDevices(listOf(TestDevices.createDevice(id = "d1", name = "Kitchen Smoke")))
            val useCase = AddBatteryEventUseCase(
                repo,
                UpdateDeviceLastReplacedUseCase(repo),
                SetDeviceNeedsBatteryUseCase(needsBattery),
            )

            useCase(TestDevices.createBatteryEvent(id = "e1", deviceId = "d1"))

            assertTrue(needsBattery.getFlaggedDeviceIds().first().isEmpty())
        }

    @Test
    fun `invoke leaves other devices' needs-battery marks alone`() =
        runTest {
            val repo = FakeDeviceRepository()
            val needsBattery = FakeNeedsBatteryRepository(initialFlagged = setOf("d1", "d2"))
            repo.setDevices(
                listOf(
                    TestDevices.createDevice(id = "d1", name = "Kitchen Smoke"),
                    TestDevices.createDevice(id = "d2", name = "Hallway CO"),
                ),
            )
            val useCase = AddBatteryEventUseCase(
                repo,
                UpdateDeviceLastReplacedUseCase(repo),
                SetDeviceNeedsBatteryUseCase(needsBattery),
            )

            useCase(TestDevices.createBatteryEvent(id = "e1", deviceId = "d1"))

            assertEquals(setOf("d2"), needsBattery.getFlaggedDeviceIds().first())
        }
}
