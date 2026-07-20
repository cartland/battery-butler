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

    /** Same scenario as the upload test: the screen closing must not lose a pending removal. */
    @Test
    fun `delete completes and clears the etag even if the calling scope is cancelled mid-flight`() =
        runTest {
            val deleteGate = CompletableDeferred<Unit>()
            val imageRepo = object : DeviceImageRepository {
                override val supported: Flow<Boolean> = flowOf(true)

                override fun observeCachedImage(imageEtag: String): Flow<DeviceImageBytes?> = flowOf(null)

                override suspend fun uploadImage(
                    deviceId: String,
                    bytes: ByteArray,
                    contentType: String,
                ): Result<String, DeviceImageError> = Result.Success("")

                override suspend fun deleteImage(deviceId: String): Boolean {
                    deleteGate.await()
                    return true
                }
            }
            val deviceRepo = FakeDeviceRepository()
            deviceRepo.setDevices(listOf(TestDevices.createDevice(id = "dev1").copy(imageEtag = "etag-1")))
            val appScope = CoroutineScope(StandardTestDispatcher(testScheduler) + Job())
            val useCase = DeleteDeviceImageUseCase(imageRepo, deviceRepo, appScope)

            val callerScope = CoroutineScope(StandardTestDispatcher(testScheduler) + Job())
            callerScope.launch { useCase("dev1") }
            testScheduler.runCurrent()

            callerScope.cancel()
            deleteGate.complete(Unit)
            testScheduler.advanceUntilIdle()

            assertNull(deviceRepo.devices.single { it.id == "dev1" }.imageEtag)
        }
}
