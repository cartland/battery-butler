package com.chriscartland.batterybutler.usecase

import com.chriscartland.batterybutler.domain.model.DeviceImageError
import com.chriscartland.batterybutler.domain.model.Result
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
import kotlin.test.assertIs
import kotlin.test.assertNull

@OptIn(ExperimentalCoroutinesApi::class)
class UploadDeviceImageUseCaseTest {
    @Test
    fun `delegates to the repository and returns its result`() =
        runTest {
            val imageRepo = FakeDeviceImageRepository().apply { uploadResult = Result.Success("etag-1") }
            val deviceRepo = FakeDeviceRepository()
            deviceRepo.setDevices(listOf(TestDevices.createDevice(id = "dev1")))
            val useCase = UploadDeviceImageUseCase(imageRepo, deviceRepo, CoroutineScope(StandardTestDispatcher(testScheduler) + Job()))

            val result = useCase("dev1", byteArrayOf(1, 2, 3), "image/jpeg")

            assertIs<Result.Success<String>>(result)
            assertEquals("etag-1", result.data)
        }

    @Test
    fun `writes the new etag onto the device on success`() =
        runTest {
            val imageRepo = FakeDeviceImageRepository().apply { uploadResult = Result.Success("etag-1") }
            val deviceRepo = FakeDeviceRepository()
            deviceRepo.setDevices(listOf(TestDevices.createDevice(id = "dev1")))
            val useCase = UploadDeviceImageUseCase(imageRepo, deviceRepo, CoroutineScope(StandardTestDispatcher(testScheduler) + Job()))

            useCase("dev1", byteArrayOf(1, 2, 3), "image/jpeg")

            assertEquals("etag-1", deviceRepo.devices.single { it.id == "dev1" }.imageEtag)
        }

    @Test
    fun `propagates a repository error without touching the device`() =
        runTest {
            val imageRepo = FakeDeviceImageRepository().apply { uploadResult = Result.Error(DeviceImageError.TooLarge()) }
            val deviceRepo = FakeDeviceRepository()
            deviceRepo.setDevices(listOf(TestDevices.createDevice(id = "dev1")))
            val useCase = UploadDeviceImageUseCase(imageRepo, deviceRepo, CoroutineScope(StandardTestDispatcher(testScheduler) + Job()))

            val result = useCase("dev1", byteArrayOf(), "image/jpeg")

            assertIs<Result.Error<DeviceImageError>>(result)
            assertIs<DeviceImageError.TooLarge>(result.error)
            assertNull(deviceRepo.devices.single { it.id == "dev1" }.imageEtag)
        }
}
