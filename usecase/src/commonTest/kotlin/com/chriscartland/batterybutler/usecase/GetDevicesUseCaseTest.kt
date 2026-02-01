package com.chriscartland.batterybutler.usecase

import com.chriscartland.batterybutler.testcommon.FakeDeviceRepository
import com.chriscartland.batterybutler.testcommon.TestDevices
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.test.runTest
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertTrue
import kotlin.time.ExperimentalTime

@OptIn(ExperimentalTime::class)
class GetDevicesUseCaseTest {
    @Test
    fun `invoke returns devices from repository`() =
        runTest {
            val device = TestDevices.createDevice(id = "1", name = "Test Device")
            val fakeRepository = FakeDeviceRepository()
            fakeRepository.setDevices(listOf(device))
            val useCase = GetDevicesUseCase(fakeRepository)

            val result = useCase().first()

            assertEquals(1, result.size)
            assertEquals(device, result[0])
        }

    @Test
    fun `invoke returns empty list when no devices`() =
        runTest {
            val fakeRepository = FakeDeviceRepository()
            val useCase = GetDevicesUseCase(fakeRepository)

            val result = useCase().first()

            assertTrue(result.isEmpty())
        }

    @Test
    fun `invoke returns multiple devices`() =
        runTest {
            val fakeRepository = FakeDeviceRepository()
            val device1 = TestDevices.createDevice(id = "1", name = "Device 1")
            val device2 = TestDevices.createDevice(id = "2", name = "Device 2")
            val device3 = TestDevices.createDevice(id = "3", name = "Device 3")
            fakeRepository.setDevices(listOf(device1, device2, device3))
            val useCase = GetDevicesUseCase(fakeRepository)

            val result = useCase().first()

            assertEquals(3, result.size)
            assertTrue(result.contains(device1))
            assertTrue(result.contains(device2))
            assertTrue(result.contains(device3))
        }

    @Test
    fun `invoke preserves device data`() =
        runTest {
            val fakeRepository = FakeDeviceRepository()
            val device = TestDevices.createDevice(
                id = "smoke-1",
                name = "Kitchen Smoke Detector",
                typeId = "type-smoke",
                location = "Kitchen",
            )
            fakeRepository.setDevices(listOf(device))
            val useCase = GetDevicesUseCase(fakeRepository)

            val result = useCase().first().first()

            assertEquals("smoke-1", result.id)
            assertEquals("Kitchen Smoke Detector", result.name)
            assertEquals("type-smoke", result.typeId)
            assertEquals("Kitchen", result.location)
        }
}
