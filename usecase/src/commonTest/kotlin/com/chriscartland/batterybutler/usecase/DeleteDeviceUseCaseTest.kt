package com.chriscartland.batterybutler.usecase

import com.chriscartland.batterybutler.testcommon.FakeDeviceRepository
import com.chriscartland.batterybutler.testcommon.FakeNeedsBatteryRepository
import com.chriscartland.batterybutler.testcommon.TestDevices
import com.chriscartland.batterybutler.usecase.SetDeviceNeedsBatteryUseCase
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.test.runTest
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertFalse
import kotlin.test.assertTrue
import kotlin.time.ExperimentalTime

@OptIn(ExperimentalTime::class)
class DeleteDeviceUseCaseTest {
    @Test
    fun `invoke deletes device from repository`() =
        runTest {
            val fakeRepository = FakeDeviceRepository()
            val device = TestDevices.createDevice(id = "1", name = "Device to delete")
            fakeRepository.devices.add(device)
            val useCase = DeleteDeviceUseCase(fakeRepository, SetDeviceNeedsBatteryUseCase(FakeNeedsBatteryRepository()))

            useCase("1")

            assertFalse(fakeRepository.devices.any { it.id == "1" })
        }

    @Test
    fun `invoke only deletes specified device`() =
        runTest {
            val fakeRepository = FakeDeviceRepository()
            val device1 = TestDevices.createDevice(id = "1", name = "Device 1")
            val device2 = TestDevices.createDevice(id = "2", name = "Device 2")
            fakeRepository.devices.addAll(listOf(device1, device2))
            val useCase = DeleteDeviceUseCase(fakeRepository, SetDeviceNeedsBatteryUseCase(FakeNeedsBatteryRepository()))

            useCase("1")

            assertEquals(1, fakeRepository.devices.size)
            assertFalse(fakeRepository.devices.any { it.id == "1" })
            assertTrue(fakeRepository.devices.any { it.id == "2" })
        }

    @Test
    fun `invoke handles non-existent device gracefully`() =
        runTest {
            val fakeRepository = FakeDeviceRepository()
            val device = TestDevices.createDevice(id = "1", name = "Existing Device")
            fakeRepository.devices.add(device)
            val useCase = DeleteDeviceUseCase(fakeRepository, SetDeviceNeedsBatteryUseCase(FakeNeedsBatteryRepository()))

            // Should not throw when deleting non-existent device
            useCase("non-existent")

            // Original device should remain
            assertEquals(1, fakeRepository.devices.size)
            assertTrue(fakeRepository.devices.any { it.id == "1" })
        }

    @Test
    fun `invoke handles empty repository`() =
        runTest {
            val fakeRepository = FakeDeviceRepository()
            val useCase = DeleteDeviceUseCase(fakeRepository, SetDeviceNeedsBatteryUseCase(FakeNeedsBatteryRepository()))

            // Should not throw when repository is empty
            useCase("any-id")

            assertTrue(fakeRepository.devices.isEmpty())
        }

    /**
     * The mark table has no foreign key to `devices`, so nothing drops the mark for us. Left
     * behind, it would silently re-attach if the same device id ever came back from a sync.
     */
    @Test
    fun `invoke clears the deleted device's needs-battery mark`() =
        runTest {
            val fakeRepository = FakeDeviceRepository()
            val needsBattery = FakeNeedsBatteryRepository(initialFlagged = setOf("d1", "d2"))
            fakeRepository.setDevices(listOf(TestDevices.createDevice(id = "d1", name = "Kitchen Smoke")))

            DeleteDeviceUseCase(fakeRepository, SetDeviceNeedsBatteryUseCase(needsBattery))("d1")

            assertEquals(setOf("d2"), needsBattery.getFlaggedDeviceIds().first())
        }
}
