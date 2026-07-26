package com.chriscartland.batterybutler.viewmodel.editdevice

import co.touchlab.kermit.Logger
import com.chriscartland.batterybutler.domain.model.DeviceImageError
import com.chriscartland.batterybutler.domain.model.DeviceInput
import com.chriscartland.batterybutler.domain.model.Result
import com.chriscartland.batterybutler.presentationmodel.editdevice.EditDeviceScreenState
import com.chriscartland.batterybutler.usecase.DeleteDeviceImageUseCase
import com.chriscartland.batterybutler.usecase.DeleteDeviceUseCase
import com.chriscartland.batterybutler.usecase.GetCachedDeviceImageUseCase
import com.chriscartland.batterybutler.usecase.GetDeviceDetailUseCase
import com.chriscartland.batterybutler.usecase.GetDeviceTypesUseCase
import com.chriscartland.batterybutler.usecase.IsDeviceImagesSupportedUseCase
import com.chriscartland.batterybutler.usecase.UpdateDeviceUseCase
import com.chriscartland.batterybutler.usecase.UploadDeviceImageUseCase
import com.chriscartland.batterybutler.viewmodel.defaultWhileSubscribed
import com.chriscartland.batterybutler.viewmodel.safeStateIn
import com.rickclephas.kmp.observableviewmodel.MutableStateFlow
import com.rickclephas.kmp.observableviewmodel.ViewModel
import com.rickclephas.kmp.observableviewmodel.coroutineScope
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.flatMapLatest
import kotlinx.coroutines.flow.flowOf
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.flow.onStart
import kotlinx.coroutines.launch
import me.tatarka.inject.annotations.Inject
import kotlin.coroutines.cancellation.CancellationException
import kotlin.time.Clock

@Inject
class EditDeviceViewModelFactory(
    private val getDeviceDetailUseCase: GetDeviceDetailUseCase,
    private val getDeviceTypesUseCase: GetDeviceTypesUseCase,
    private val updateDeviceUseCase: UpdateDeviceUseCase,
    private val deleteDeviceUseCase: DeleteDeviceUseCase,
    private val getCachedDeviceImageUseCase: GetCachedDeviceImageUseCase,
    private val uploadDeviceImageUseCase: UploadDeviceImageUseCase,
    private val deleteDeviceImageUseCase: DeleteDeviceImageUseCase,
    private val isDeviceImagesSupportedUseCase: IsDeviceImagesSupportedUseCase,
) {
    fun create(deviceId: String): EditDeviceViewModel =
        EditDeviceViewModel(
            deviceId,
            getDeviceDetailUseCase,
            getDeviceTypesUseCase,
            updateDeviceUseCase,
            deleteDeviceUseCase,
            getCachedDeviceImageUseCase,
            uploadDeviceImageUseCase,
            deleteDeviceImageUseCase,
            isDeviceImagesSupportedUseCase,
        )
}

@OptIn(ExperimentalCoroutinesApi::class)
class EditDeviceViewModel(
    private val deviceId: String,
    private val getDeviceDetailUseCase: GetDeviceDetailUseCase,
    private val getDeviceTypesUseCase: GetDeviceTypesUseCase,
    private val updateDeviceUseCase: UpdateDeviceUseCase,
    private val deleteDeviceUseCase: DeleteDeviceUseCase,
    private val getCachedDeviceImageUseCase: GetCachedDeviceImageUseCase,
    private val uploadDeviceImageUseCase: UploadDeviceImageUseCase,
    private val deleteDeviceImageUseCase: DeleteDeviceImageUseCase,
    private val isDeviceImagesSupportedUseCase: IsDeviceImagesSupportedUseCase,
) : ViewModel() {
    val uiState: StateFlow<EditDeviceScreenState> = combine(
        getDeviceDetailUseCase(deviceId),
        getDeviceTypesUseCase(),
        isDeviceImagesSupportedUseCase(),
    ) { device, types, imagesSupported -> Triple(device, types, imagesSupported) }
        .flatMapLatest { (device, types, imagesSupported) ->
            if (device == null) {
                flowOf(EditDeviceScreenState.NotFound)
            } else {
                val imageEtag = device.imageEtag
                // Seed with null so Success is produced from device/type data immediately and the
                // photo folds in when the cache emits. Without the seed, a slow or empty image flow
                // withholds the first emission and wedges the screen at Loading (the bytes are a
                // decoration -- they must never gate the screen). See DeviceDetailViewModel.
                val imageBytesFlow = if (imageEtag != null) {
                    getCachedDeviceImageUseCase(imageEtag).onStart { emit(null) }
                } else {
                    flowOf(null)
                }
                imageBytesFlow.map { imageBytes ->
                    EditDeviceScreenState.Success(
                        device = device,
                        deviceTypes = types,
                        imagesSupported = imagesSupported,
                        imageBytes = imageBytes,
                    )
                }
            }
        }.safeStateIn(
            viewModelScope = viewModelScope,
            started = defaultWhileSubscribed(),
            initialValue = EditDeviceScreenState.Loading,
            // See DeviceTypeDetailViewModel: transient DB failure -> NotFound (logged), not a wedge.
            onError = { EditDeviceScreenState.NotFound },
        )

    private val _photoError = MutableStateFlow<DeviceImageError?>(viewModelScope, null)
    val photoError: StateFlow<DeviceImageError?> = _photoError

    /** True while a photo upload or removal is in flight, for a loading indicator + double-tap guard. */
    private val _photoUploading = MutableStateFlow(viewModelScope, false)
    val photoUploading: StateFlow<Boolean> = _photoUploading

    /**
     * Set true for a brief moment after a successful photo upload, so the UI can show a transient
     * "Photo updated" confirmation. This matters for a *same-photo* re-upload: the avatar is
     * visually identical, so without an explicit cue the user gets no signal it worked. The UI
     * clears it (via [clearPhotoUpdated]) after showing it; it's also reset at the start of each
     * upload so a repeat upload re-triggers a fresh confirmation.
     */
    private val _photoUpdated = MutableStateFlow(viewModelScope, false)
    val photoUpdated: StateFlow<Boolean> = _photoUpdated

    fun updateDevice(input: DeviceInput) {
        val currentState = uiState.value
        if (currentState is EditDeviceScreenState.Success) {
            viewModelScope.coroutineScope.launch {
                val updatedDevice = currentState.device.copy(
                    name = input.name,
                    location = input.location,
                    typeId = input.typeId,
                    imagePath = input.imagePath ?: currentState.device.imagePath,
                    lastUpdated = Clock.System.now(),
                )
                updateDeviceUseCase(updatedDevice)
            }
        }
    }

    fun deleteDevice() {
        viewModelScope.coroutineScope.launch {
            deleteDeviceUseCase(deviceId)
        }
    }

    /**
     * Uploads a photo already picked and normalized by the UI layer. The upload itself (and
     * recording the new etag) runs on [UploadDeviceImageUseCase]'s own app-scoped coroutine, so it
     * completes even if this screen closes before it's done -- only [photoUploading]/[photoError]
     * are tied to this ViewModel's lifetime. [photoUploading] is cleared in a `finally` so an
     * unexpected exception (not just a [Result.Error]) can never leave the spinner stuck forever.
     */
    fun uploadPhoto(
        bytes: ByteArray,
        contentType: String,
    ) {
        viewModelScope.coroutineScope.launch {
            _photoError.value = null
            _photoUpdated.value = false
            _photoUploading.value = true
            try {
                when (val result = uploadDeviceImageUseCase(deviceId, bytes, contentType)) {
                    is Result.Success -> _photoUpdated.value = true
                    is Result.Error -> _photoError.value = result.error
                }
            } catch (e: CancellationException) {
                throw e
            } catch (e: Exception) {
                Logger.e(TAG, e) { "uploadPhoto($deviceId) failed unexpectedly" }
                _photoError.value = DeviceImageError.NetworkError(cause = e.message)
            } finally {
                _photoUploading.value = false
            }
        }
    }

    fun removePhoto() {
        viewModelScope.coroutineScope.launch {
            _photoError.value = null
            _photoUploading.value = true
            try {
                if (!deleteDeviceImageUseCase(deviceId)) {
                    _photoError.value = DeviceImageError.NetworkError(message = "Failed to remove photo")
                }
            } catch (e: CancellationException) {
                throw e
            } catch (e: Exception) {
                Logger.e(TAG, e) { "removePhoto($deviceId) failed unexpectedly" }
                _photoError.value = DeviceImageError.NetworkError(cause = e.message)
            } finally {
                _photoUploading.value = false
            }
        }
    }

    fun clearPhotoError() {
        _photoError.value = null
    }

    /** Called by the UI once it has shown the transient "Photo updated" confirmation. */
    fun clearPhotoUpdated() {
        _photoUpdated.value = false
    }

    /** The UI layer picked bytes it couldn't decode/normalize locally -- surface it like any other photo error. */
    fun reportPhotoPickFailed() {
        _photoError.value = DeviceImageError.InvalidImage()
    }

    private companion object {
        const val TAG = "EditDeviceViewModel"
    }
}
