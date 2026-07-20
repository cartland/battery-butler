package com.chriscartland.batterybutler.usecase

import com.chriscartland.batterybutler.domain.model.DeviceImageBytes
import com.chriscartland.batterybutler.domain.model.DeviceImageError
import com.chriscartland.batterybutler.domain.model.Result
import com.chriscartland.batterybutler.domain.repository.DeviceImageRepository
import com.chriscartland.batterybutler.testcommon.FakeDeviceImageRepository
import com.chriscartland.batterybutler.testcommon.FakeDeviceRepository
import com.chriscartland.batterybutler.testcommon.TestDevices
import kotlinx.coroutines.CompletableDeferred
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.Job
import kotlinx.coroutines.cancel
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.flowOf
import kotlinx.coroutines.launch
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

    /**
     * This is the actual bug that shipped in android/51: a screen closing (Save/Back) cancels its
     * own scope mid-upload. Proves the fix by simulating exactly that -- a "caller" scope (standing
     * in for `viewModelScope`) is cancelled while the upload is suspended mid-flight, and the etag
     * must still land, because [UploadDeviceImageUseCase] runs the real work on its own [scope], not
     * the caller's.
     */
    @Test
    fun `upload completes and writes the etag even if the calling scope is cancelled mid-flight`() =
        runTest {
            val uploadGate = CompletableDeferred<Unit>()
            val imageRepo = object : DeviceImageRepository {
                override val supported: Flow<Boolean> = flowOf(true)

                override fun observeCachedImage(imageEtag: String): Flow<DeviceImageBytes?> = flowOf(null)

                override suspend fun uploadImage(
                    deviceId: String,
                    bytes: ByteArray,
                    contentType: String,
                ): Result<String, DeviceImageError> {
                    uploadGate.await()
                    return Result.Success("etag-1")
                }

                override suspend fun deleteImage(deviceId: String): Boolean = true
            }
            val deviceRepo = FakeDeviceRepository()
            deviceRepo.setDevices(listOf(TestDevices.createDevice(id = "dev1")))
            val appScope = CoroutineScope(StandardTestDispatcher(testScheduler) + Job())
            val useCase = UploadDeviceImageUseCase(imageRepo, deviceRepo, appScope)

            // Stands in for viewModelScope: the scope that launches the upload and gets cancelled
            // when the screen closes, before the upload (paused on uploadGate) has finished.
            val callerScope = CoroutineScope(StandardTestDispatcher(testScheduler) + Job())
            callerScope.launch { useCase("dev1", byteArrayOf(1, 2, 3), "image/jpeg") }
            testScheduler.runCurrent()

            callerScope.cancel()
            uploadGate.complete(Unit)
            testScheduler.advanceUntilIdle()

            assertEquals("etag-1", deviceRepo.devices.single { it.id == "dev1" }.imageEtag)
        }
}
