package com.chriscartland.batterybutler.usecase

import com.chriscartland.batterybutler.testcommon.FakeDeviceRepository
import com.chriscartland.batterybutler.testcommon.TestDevices
import kotlinx.coroutines.test.runTest
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertTrue
import kotlin.time.ExperimentalTime

@OptIn(ExperimentalTime::class)
class BuildAiContextUseCaseTest {
    @Test
    fun `produces inventory counts`() =
        runTest {
            val repo = FakeDeviceRepository()
            repo.setDevices(
                listOf(
                    TestDevices.createDevice(id = "d1", name = "Device 1"),
                    TestDevices.createDevice(id = "d2", name = "Device 2"),
                ),
            )
            val useCase = BuildAiContextUseCase(repo)

            val result = useCase()

            assertTrue(result.contains("Devices (2)"))
        }

    @Test
    fun `resolves type names`() =
        runTest {
            val repo = FakeDeviceRepository()
            repo.setDeviceTypes(
                listOf(TestDevices.createDeviceType(id = "t1", name = "Smoke Detector")),
            )
            repo.setDevices(
                listOf(TestDevices.createDevice(id = "d1", typeId = "t1")),
            )
            val useCase = BuildAiContextUseCase(repo)

            val result = useCase()

            assertTrue(result.contains("Smoke Detector"))
        }

    @Test
    fun `includes locations`() =
        runTest {
            val repo = FakeDeviceRepository()
            repo.setDevices(
                listOf(TestDevices.createDevice(id = "d1", location = "Kitchen")),
            )
            val useCase = BuildAiContextUseCase(repo)

            val result = useCase()

            assertTrue(result.contains("Kitchen"))
        }

    @Test
    fun `limits events to 20 recent`() =
        runTest {
            val repo = FakeDeviceRepository()
            repo.setDevices(
                listOf(TestDevices.createDevice(id = "d1", name = "Sensor")),
            )
            val events = (1..25).map { i ->
                TestDevices.createBatteryEvent(id = "e-$i", deviceId = "d1")
            }
            repo.setEvents(events)
            val useCase = BuildAiContextUseCase(repo)

            val result = useCase()

            val replacementsSection = result.substringAfter("Recent Battery Replacements:")
            val eventLines = replacementsSection.lines().filter { it.trimStart().startsWith("- ") }
            assertEquals(20, eventLines.size)
        }

    @Test
    fun `handles empty repository`() =
        runTest {
            val repo = FakeDeviceRepository()
            val useCase = BuildAiContextUseCase(repo)

            val result = useCase()

            assertTrue(result.contains("Devices (0)"))
            assertTrue(result.contains("Device Types (0)"))
        }
}
