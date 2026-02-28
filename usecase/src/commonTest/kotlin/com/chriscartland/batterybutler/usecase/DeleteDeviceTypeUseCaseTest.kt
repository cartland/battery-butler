package com.chriscartland.batterybutler.usecase

import com.chriscartland.batterybutler.testcommon.FakeDeviceRepository
import com.chriscartland.batterybutler.testcommon.TestDevices
import kotlinx.coroutines.test.runTest
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertFalse
import kotlin.test.assertTrue
import kotlin.time.ExperimentalTime

@OptIn(ExperimentalTime::class)
class DeleteDeviceTypeUseCaseTest {
    @Test
    fun `invoke deletes device type from repository`() =
        runTest {
            val fakeRepository = FakeDeviceRepository()
            val deviceType = TestDevices.createDeviceType(id = "t1", name = "Smoke Detector")
            fakeRepository.setDeviceTypes(listOf(deviceType))
            val useCase = DeleteDeviceTypeUseCase(fakeRepository)

            useCase("t1")

            assertTrue(fakeRepository.deviceTypes.isEmpty())
        }

    @Test
    fun `invoke only deletes matching type`() =
        runTest {
            val fakeRepository = FakeDeviceRepository()
            val type1 = TestDevices.createDeviceType(id = "t1", name = "Smoke Detector")
            val type2 = TestDevices.createDeviceType(id = "t2", name = "Remote Control")
            fakeRepository.setDeviceTypes(listOf(type1, type2))
            val useCase = DeleteDeviceTypeUseCase(fakeRepository)

            useCase("t1")

            assertEquals(1, fakeRepository.deviceTypes.size)
            assertFalse(fakeRepository.deviceTypes.any { it.id == "t1" })
            assertTrue(fakeRepository.deviceTypes.any { it.id == "t2" })
        }
}
