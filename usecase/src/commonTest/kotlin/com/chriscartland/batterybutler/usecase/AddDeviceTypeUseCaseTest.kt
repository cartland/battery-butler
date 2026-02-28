package com.chriscartland.batterybutler.usecase

import com.chriscartland.batterybutler.testcommon.FakeDeviceRepository
import com.chriscartland.batterybutler.testcommon.TestDevices
import kotlinx.coroutines.test.runTest
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertTrue
import kotlin.time.ExperimentalTime

@OptIn(ExperimentalTime::class)
class AddDeviceTypeUseCaseTest {
    @Test
    fun `invoke adds device type to repository`() =
        runTest {
            val fakeRepository = FakeDeviceRepository()
            val useCase = AddDeviceTypeUseCase(fakeRepository)
            val deviceType = TestDevices.createDeviceType(id = "t1", name = "Smoke Detector")

            useCase(deviceType)

            assertTrue(fakeRepository.deviceTypes.contains(deviceType))
        }

    @Test
    fun `invoke preserves all properties`() =
        runTest {
            val fakeRepository = FakeDeviceRepository()
            val useCase = AddDeviceTypeUseCase(fakeRepository)
            val deviceType = TestDevices.createDeviceType(
                id = "type-remote",
                name = "TV Remote",
                defaultIcon = "remote_control",
                batteryType = "AAA",
                batteryQuantity = 4,
            )

            useCase(deviceType)

            val addedType = fakeRepository.deviceTypes.first()
            assertEquals("type-remote", addedType.id)
            assertEquals("TV Remote", addedType.name)
            assertEquals("remote_control", addedType.defaultIcon)
            assertEquals("AAA", addedType.batteryType)
            assertEquals(4, addedType.batteryQuantity)
        }
}
