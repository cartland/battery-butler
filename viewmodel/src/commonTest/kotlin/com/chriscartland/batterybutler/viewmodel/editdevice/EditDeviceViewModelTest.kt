package com.chriscartland.batterybutler.viewmodel.editdevice

import com.chriscartland.batterybutler.domain.model.DeviceImageError
import com.chriscartland.batterybutler.domain.model.DeviceInput
import com.chriscartland.batterybutler.domain.model.DeviceType
import com.chriscartland.batterybutler.domain.model.Result
import com.chriscartland.batterybutler.presentationmodel.editdevice.EditDeviceScreenState
import com.chriscartland.batterybutler.testcommon.FakeDeviceImageRepository
import com.chriscartland.batterybutler.testcommon.FakeDeviceRepository
import com.chriscartland.batterybutler.testcommon.TestDevices
import com.chriscartland.batterybutler.usecase.DeleteDeviceImageUseCase
import com.chriscartland.batterybutler.usecase.DeleteDeviceUseCase
import com.chriscartland.batterybutler.usecase.GetCachedDeviceImageUseCase
import com.chriscartland.batterybutler.usecase.GetDeviceDetailUseCase
import com.chriscartland.batterybutler.usecase.GetDeviceTypesUseCase
import com.chriscartland.batterybutler.usecase.IsDeviceImagesSupportedUseCase
import com.chriscartland.batterybutler.usecase.UpdateDeviceUseCase
import com.chriscartland.batterybutler.usecase.UploadDeviceImageUseCase
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.Job
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.test.StandardTestDispatcher
import kotlinx.coroutines.test.resetMain
import kotlinx.coroutines.test.runTest
import kotlinx.coroutines.test.setMain
import kotlin.test.AfterTest
import kotlin.test.BeforeTest
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertIs
import kotlin.test.assertNull
import kotlin.test.assertTrue

@OptIn(ExperimentalCoroutinesApi::class)
class EditDeviceViewModelTest {
    private val testDispatcher = StandardTestDispatcher()

    @BeforeTest
    fun setUp() {
        Dispatchers.setMain(testDispatcher)
    }

    @AfterTest
    fun tearDown() {
        Dispatchers.resetMain()
    }

    @Test
    fun `initial state is Loading`() =
        runTest {
            val repo = FakeDeviceRepository()
            val viewModel = createViewModel(repo, "device-1")

            assertEquals(EditDeviceScreenState.Loading, viewModel.uiState.value)
        }

    @Test
    fun `device found emits Success state`() =
        runTest {
            val repo = FakeDeviceRepository()
            val device = TestDevices.createDevice(id = "device-1", name = "Smoke Detector", typeId = "type-1")
            val type = DeviceType(id = "type-1", name = "Smoke Detector Type")
            repo.setDevices(listOf(device))
            repo.setDeviceTypes(listOf(type))

            val viewModel = createViewModel(repo, "device-1")

            val state = viewModel.uiState.first { it is EditDeviceScreenState.Success }
            assertIs<EditDeviceScreenState.Success>(state)
            assertEquals("Smoke Detector", state.device.name)
            assertEquals(1, state.deviceTypes.size)
            assertEquals("Smoke Detector Type", state.deviceTypes[0].name)
        }

    @Test
    fun `device not found emits NotFound state`() =
        runTest {
            val repo = FakeDeviceRepository()
            // No devices in repo

            val viewModel = createViewModel(repo, "nonexistent-id")

            val state = viewModel.uiState.first { it is EditDeviceScreenState.NotFound }
            assertIs<EditDeviceScreenState.NotFound>(state)
        }

    @Test
    fun `updateDevice updates device in repository`() =
        runTest {
            val repo = FakeDeviceRepository()
            val device = TestDevices.createDevice(id = "device-1", name = "Old Name", typeId = "type-1")
            val type = DeviceType(id = "type-1", name = "Test Type")
            repo.setDevices(listOf(device))
            repo.setDeviceTypes(listOf(type))

            val viewModel = createViewModel(repo, "device-1")

            // Wait for Success state
            viewModel.uiState.first { it is EditDeviceScreenState.Success }

            val input = DeviceInput(
                name = "New Name",
                location = "Kitchen",
                typeId = "type-1",
            )
            viewModel.updateDevice(input)
            testDispatcher.scheduler.advanceUntilIdle()

            assertEquals("New Name", repo.devices[0].name)
            assertEquals("Kitchen", repo.devices[0].location)
        }

    @Test
    fun `deleteDevice removes device from repository`() =
        runTest {
            val repo = FakeDeviceRepository()
            val device = TestDevices.createDevice(id = "device-1", name = "Test Device")
            repo.setDevices(listOf(device))

            val viewModel = createViewModel(repo, "device-1")

            viewModel.deleteDevice()
            testDispatcher.scheduler.advanceUntilIdle()

            assertTrue(repo.devices.isEmpty())
        }

    @Test
    fun `uploadPhoto stores the new etag on the device after a successful upload`() =
        runTest {
            val repo = FakeDeviceRepository()
            val device = TestDevices.createDevice(id = "device-1", name = "Test Device")
            repo.setDevices(listOf(device))
            val imageRepo = FakeDeviceImageRepository().apply { uploadResult = Result.Success("etag-1") }

            val viewModel = createViewModel(repo, "device-1", imageRepo)
            viewModel.uiState.first { it is EditDeviceScreenState.Success }

            viewModel.uploadPhoto(byteArrayOf(1, 2, 3), "image/jpeg")
            testDispatcher.scheduler.advanceUntilIdle()

            assertEquals("etag-1", repo.devices[0].imageEtag)
            assertNull(viewModel.photoError.value)
            assertEquals(false, viewModel.photoUploading.value)
        }

    @Test
    fun `uploadPhoto surfaces a repository error without touching the device`() =
        runTest {
            val repo = FakeDeviceRepository()
            val device = TestDevices.createDevice(id = "device-1", name = "Test Device")
            repo.setDevices(listOf(device))
            val imageRepo = FakeDeviceImageRepository().apply {
                uploadResult = Result.Error(DeviceImageError.TooLarge())
            }

            val viewModel = createViewModel(repo, "device-1", imageRepo)
            viewModel.uiState.first { it is EditDeviceScreenState.Success }

            viewModel.uploadPhoto(byteArrayOf(1, 2, 3), "image/jpeg")
            testDispatcher.scheduler.advanceUntilIdle()

            assertIs<DeviceImageError.TooLarge>(viewModel.photoError.value)
            assertNull(repo.devices[0].imageEtag)
            assertEquals(false, viewModel.photoUploading.value)
        }

    /**
     * Regression test: a real-world upload threw an unexpected exception (not a typed
     * [DeviceImageError.NetworkError]) after the byte upload itself had already succeeded
     * server-side, and [photoUploading] never got cleared -- the earlier version of `uploadPhoto`
     * had no try/finally, so anything other than a [Result] return value left the spinner stuck
     * forever with no error shown.
     */
    @Test
    fun `uploadPhoto clears photoUploading and surfaces an error when the use case throws unexpectedly`() =
        runTest {
            val repo = FakeDeviceRepository()
            val device = TestDevices.createDevice(id = "device-1", name = "Test Device")
            repo.setDevices(listOf(device))
            val imageRepo = FakeDeviceImageRepository().apply { uploadThrows = RuntimeException("boom") }

            val viewModel = createViewModel(repo, "device-1", imageRepo)
            viewModel.uiState.first { it is EditDeviceScreenState.Success }

            viewModel.uploadPhoto(byteArrayOf(1, 2, 3), "image/jpeg")
            testDispatcher.scheduler.advanceUntilIdle()

            assertEquals(false, viewModel.photoUploading.value)
            assertIs<DeviceImageError.NetworkError>(viewModel.photoError.value)
            assertNull(repo.devices[0].imageEtag)
        }

    @Test
    fun `removePhoto clears the device's imageEtag`() =
        runTest {
            val repo = FakeDeviceRepository()
            val device = TestDevices.createDevice(id = "device-1", name = "Test Device").copy(imageEtag = "etag-1")
            repo.setDevices(listOf(device))
            val imageRepo = FakeDeviceImageRepository()

            val viewModel = createViewModel(repo, "device-1", imageRepo)
            viewModel.uiState.first { it is EditDeviceScreenState.Success }

            viewModel.removePhoto()
            testDispatcher.scheduler.advanceUntilIdle()

            assertEquals(listOf("device-1"), imageRepo.deletedDeviceIds)
            assertNull(repo.devices[0].imageEtag)
            assertEquals(false, viewModel.photoUploading.value)
        }

    /** Same regression as the upload case, for `removePhoto`. */
    @Test
    fun `removePhoto clears photoUploading and surfaces an error when the use case throws unexpectedly`() =
        runTest {
            val repo = FakeDeviceRepository()
            val device = TestDevices.createDevice(id = "device-1", name = "Test Device").copy(imageEtag = "etag-1")
            repo.setDevices(listOf(device))
            val imageRepo = FakeDeviceImageRepository().apply { deleteThrows = RuntimeException("boom") }

            val viewModel = createViewModel(repo, "device-1", imageRepo)
            viewModel.uiState.first { it is EditDeviceScreenState.Success }

            viewModel.removePhoto()
            testDispatcher.scheduler.advanceUntilIdle()

            assertEquals(false, viewModel.photoUploading.value)
            assertIs<DeviceImageError.NetworkError>(viewModel.photoError.value)
            assertEquals("etag-1", repo.devices[0].imageEtag)
        }

    @Test
    fun `reportPhotoPickFailed surfaces an InvalidImage error without touching the device`() =
        runTest {
            val repo = FakeDeviceRepository()
            val device = TestDevices.createDevice(id = "device-1", name = "Test Device")
            repo.setDevices(listOf(device))

            val viewModel = createViewModel(repo, "device-1")
            viewModel.uiState.first { it is EditDeviceScreenState.Success }

            viewModel.reportPhotoPickFailed()

            assertIs<DeviceImageError.InvalidImage>(viewModel.photoError.value)
            assertNull(repo.devices[0].imageEtag)
        }

    private fun createViewModel(
        repo: FakeDeviceRepository,
        deviceId: String,
        imageRepo: FakeDeviceImageRepository = FakeDeviceImageRepository(),
    ): EditDeviceViewModel {
        val scope = CoroutineScope(testDispatcher + Job())
        return EditDeviceViewModel(
            deviceId = deviceId,
            getDeviceDetailUseCase = GetDeviceDetailUseCase(repo),
            getDeviceTypesUseCase = GetDeviceTypesUseCase(repo),
            updateDeviceUseCase = UpdateDeviceUseCase(repo),
            deleteDeviceUseCase = DeleteDeviceUseCase(repo),
            getCachedDeviceImageUseCase = GetCachedDeviceImageUseCase(imageRepo),
            uploadDeviceImageUseCase = UploadDeviceImageUseCase(imageRepo, repo, scope),
            deleteDeviceImageUseCase = DeleteDeviceImageUseCase(imageRepo, repo, scope),
            isDeviceImagesSupportedUseCase = IsDeviceImagesSupportedUseCase(imageRepo),
        )
    }
}
