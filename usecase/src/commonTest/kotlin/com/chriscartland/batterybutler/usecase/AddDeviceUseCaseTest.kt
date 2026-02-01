package com.chriscartland.batterybutler.usecase

import com.chriscartland.batterybutler.testcommon.FakeDeviceRepository
import com.chriscartland.batterybutler.testcommon.TestDevices
import kotlinx.coroutines.test.runTest
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertTrue
import kotlin.time.ExperimentalTime
import kotlin.time.Instant

@OptIn(ExperimentalTime::class)
class AddDeviceUseCaseTest {
    @Test
    fun `invoke adds device to repository`() =
        runTest {
            val fakeRepository = FakeDeviceRepository()
            val useCase = AddDeviceUseCase(fakeRepository)
            val device = TestDevices.createDevice(id = "1", name = "Test Device")

            useCase(device)

            assertTrue(fakeRepository.devices.contains(device))
        }

    @Test
    fun `invoke adds multiple devices`() =
        runTest {
            val fakeRepository = FakeDeviceRepository()
            val useCase = AddDeviceUseCase(fakeRepository)
            val device1 = TestDevices.createDevice(id = "1", name = "Device 1")
            val device2 = TestDevices.createDevice(id = "2", name = "Device 2")

            useCase(device1)
            useCase(device2)

            assertEquals(2, fakeRepository.devices.size)
            assertTrue(fakeRepository.devices.contains(device1))
            assertTrue(fakeRepository.devices.contains(device2))
        }

    @Test
    fun `invoke preserves device properties`() =
        runTest {
            val fakeRepository = FakeDeviceRepository()
            val useCase = AddDeviceUseCase(fakeRepository)
            val device = TestDevices.createDevice(
                id = "test-id",
                name = "Smoke Detector",
                typeId = "type-smoke",
                location = "Kitchen",
                batteryLastReplaced = Instant.parse("2023-06-15T10:30:00Z"),
            )

            useCase(device)

            val addedDevice = fakeRepository.devices.first()
            assertEquals("test-id", addedDevice.id)
            assertEquals("Smoke Detector", addedDevice.name)
            assertEquals("type-smoke", addedDevice.typeId)
            assertEquals("Kitchen", addedDevice.location)
            assertEquals(Instant.parse("2023-06-15T10:30:00Z"), addedDevice.batteryLastReplaced)
        }
}
