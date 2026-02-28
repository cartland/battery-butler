package com.chriscartland.batterybutler.usecase

import com.chriscartland.batterybutler.testcommon.FakeDeviceRepository
import com.chriscartland.batterybutler.testcommon.TestDevices
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
            val useCase = AddBatteryEventUseCase(repo, updateLastReplaced)
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
            val useCase = AddBatteryEventUseCase(repo, updateLastReplaced)
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
}
