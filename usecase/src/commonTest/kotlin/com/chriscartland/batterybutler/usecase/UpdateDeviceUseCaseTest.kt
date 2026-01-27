package com.chriscartland.batterybutler.usecase

import kotlinx.coroutines.test.runTest
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.time.ExperimentalTime
import kotlin.time.Instant

@OptIn(ExperimentalTime::class)
class UpdateDeviceUseCaseTest {
    @Test
    fun `invoke updates device in repository`() =
        runTest {
            val fakeRepository = FakeDeviceRepository()
            val originalDevice = TestDevices.createDevice(id = "1", name = "Original Name")
            fakeRepository.devices.add(originalDevice)
            val useCase = UpdateDeviceUseCase(fakeRepository)
            val updatedDevice = TestDevices.createDevice(id = "1", name = "Updated Name")

            useCase(updatedDevice)

            assertEquals(1, fakeRepository.devices.size)
            assertEquals("Updated Name", fakeRepository.devices.first().name)
        }

    @Test
    fun `invoke updates device location`() =
        runTest {
            val fakeRepository = FakeDeviceRepository()
            val originalDevice = TestDevices.createDevice(id = "1", name = "Device", location = "Kitchen")
            fakeRepository.devices.add(originalDevice)
            val useCase = UpdateDeviceUseCase(fakeRepository)
            val updatedDevice = TestDevices.createDevice(id = "1", name = "Device", location = "Living Room")

            useCase(updatedDevice)

            assertEquals("Living Room", fakeRepository.devices.first().location)
        }

    @Test
    fun `invoke updates battery replacement date`() =
        runTest {
            val fakeRepository = FakeDeviceRepository()
            val oldDate = Instant.parse("2023-01-01T00:00:00Z")
            val newDate = Instant.parse("2024-06-15T10:30:00Z")
            val originalDevice = TestDevices.createDevice(id = "1", name = "Device", batteryLastReplaced = oldDate)
            fakeRepository.devices.add(originalDevice)
            val useCase = UpdateDeviceUseCase(fakeRepository)
            val updatedDevice = TestDevices.createDevice(id = "1", name = "Device", batteryLastReplaced = newDate)

            useCase(updatedDevice)

            assertEquals(newDate, fakeRepository.devices.first().batteryLastReplaced)
        }

    @Test
    fun `invoke does not affect other devices`() =
        runTest {
            val fakeRepository = FakeDeviceRepository()
            val device1 = TestDevices.createDevice(id = "1", name = "Device 1")
            val device2 = TestDevices.createDevice(id = "2", name = "Device 2")
            fakeRepository.devices.addAll(listOf(device1, device2))
            val useCase = UpdateDeviceUseCase(fakeRepository)
            val updatedDevice1 = TestDevices.createDevice(id = "1", name = "Updated Device 1")

            useCase(updatedDevice1)

            assertEquals(2, fakeRepository.devices.size)
            assertEquals("Updated Device 1", fakeRepository.devices.find { it.id == "1" }?.name)
            assertEquals("Device 2", fakeRepository.devices.find { it.id == "2" }?.name)
        }
}
