package com.chriscartland.batterybutler.viewmodel.editdevice

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
import kotlinx.coroutines.launch
import me.tatarka.inject.annotations.Inject
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
                val imageBytesFlow = if (imageEtag != null) getCachedDeviceImageUseCase(imageEtag) else flowOf(null)
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
        )

    private val _photoError = MutableStateFlow<DeviceImageError?>(viewModelScope, null)
    val photoError: StateFlow<DeviceImageError?> = _photoError

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

    /** Uploads a photo already picked and normalized by the UI layer, then records the new etag. */
    fun uploadPhoto(
        bytes: ByteArray,
        contentType: String,
    ) {
        viewModelScope.coroutineScope.launch {
            when (val result = uploadDeviceImageUseCase(deviceId, bytes, contentType)) {
                is Result.Success -> {
                    _photoError.value = null
                    applyImageEtag(result.data)
                }

                is Result.Error -> {
                    _photoError.value = result.error
                }
            }
        }
    }

    fun removePhoto() {
        viewModelScope.coroutineScope.launch {
            if (deleteDeviceImageUseCase(deviceId)) {
                _photoError.value = null
                applyImageEtag(null)
            } else {
                _photoError.value = DeviceImageError.NetworkError(message = "Failed to remove photo")
            }
        }
    }

    fun clearPhotoError() {
        _photoError.value = null
    }

    private suspend fun applyImageEtag(imageEtag: String?) {
        val current = (uiState.value as? EditDeviceScreenState.Success)?.device ?: return
        updateDeviceUseCase(current.copy(imageEtag = imageEtag, lastUpdated = Clock.System.now()))
    }
}
