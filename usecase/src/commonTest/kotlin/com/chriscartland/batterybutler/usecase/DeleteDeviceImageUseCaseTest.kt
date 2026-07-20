package com.chriscartland.batterybutler.usecase

import com.chriscartland.batterybutler.testcommon.FakeDeviceImageRepository
import com.chriscartland.batterybutler.testcommon.FakeDeviceRepository
import com.chriscartland.batterybutler.testcommon.TestDevices
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.Job
import kotlinx.coroutines.test.StandardTestDispatcher
import kotlinx.coroutines.test.runTest
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertNull
import kotlin.test.assertTrue

@OptIn(ExperimentalCoroutinesApi::class)
class DeleteDeviceImageUseCaseTest {
    @Test
    fun `delegates to the repository and clears the device's etag`() =
        runTest {
            val imageRepo = FakeDeviceImageRepository()
            val deviceRepo = FakeDeviceRepository()
            deviceRepo.setDevices(listOf(TestDevices.createDevice(id = "dev1").copy(imageEtag = "etag-1")))
            val useCase = DeleteDeviceImageUseCase(imageRepo, deviceRepo, CoroutineScope(StandardTestDispatcher(testScheduler) + Job()))

            val result = useCase("dev1")

            assertTrue(result)
            assertEquals(listOf("dev1"), imageRepo.deletedDeviceIds)
            assertNull(deviceRepo.devices.single { it.id == "dev1" }.imageEtag)
        }

    @Test
    fun `propagates a repository failure without touching the device`() =
        runTest {
            val imageRepo = FakeDeviceImageRepository().apply { deleteResult = false }
            val deviceRepo = FakeDeviceRepository()
            deviceRepo.setDevices(listOf(TestDevices.createDevice(id = "dev1").copy(imageEtag = "etag-1")))
            val useCase = DeleteDeviceImageUseCase(imageRepo, deviceRepo, CoroutineScope(StandardTestDispatcher(testScheduler) + Job()))

            assertEquals(false, useCase("dev1"))
            assertEquals("etag-1", deviceRepo.devices.single { it.id == "dev1" }.imageEtag)
        }
}
