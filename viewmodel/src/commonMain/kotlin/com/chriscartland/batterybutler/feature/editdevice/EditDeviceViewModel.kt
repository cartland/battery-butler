package com.chriscartland.batterybutler.feature.editdevice

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.chriscartland.batterybutler.domain.model.Device
import com.chriscartland.batterybutler.domain.model.DeviceInput
import com.chriscartland.batterybutler.domain.model.DeviceType
import com.chriscartland.batterybutler.domain.repository.DeviceRepository
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.launch
import kotlinx.datetime.Clock
import me.tatarka.inject.annotations.Inject

@Inject
class EditDeviceViewModelFactory(
    private val deviceRepository: DeviceRepository,
) {
    fun create(deviceId: String): EditDeviceViewModel = EditDeviceViewModel(deviceId, deviceRepository)
}

class EditDeviceViewModel(
    private val deviceId: String,
    private val deviceRepository: DeviceRepository,
) : ViewModel() {
    val uiState: StateFlow<EditDeviceUiState> = combine(
        deviceRepository.getDeviceById(deviceId),
        deviceRepository.getAllDeviceTypes(),
    ) { device, types ->
        if (device == null) {
            EditDeviceUiState.NotFound
        } else {
            EditDeviceUiState.Success(
                device = device,
                deviceTypes = types,
            )
        }
    }.stateIn(
        scope = viewModelScope,
        started = SharingStarted.WhileSubscribed(5000),
        initialValue = EditDeviceUiState.Loading,
    )

    fun updateDevice(input: DeviceInput) {
        val currentState = uiState.value
        if (currentState is EditDeviceUiState.Success) {
            viewModelScope.launch {
                val updatedDevice = currentState.device.copy(
                    name = input.name,
                    location = input.location,
                    typeId = input.typeId,
                    imagePath = input.imagePath ?: currentState.device.imagePath,
                    lastUpdated = Clock.System.now(),
                )
                deviceRepository.updateDevice(updatedDevice)
            }
        }
    }

    fun deleteDevice() {
        viewModelScope.launch {
            deviceRepository.deleteDevice(deviceId)
        }
    }
}

sealed interface EditDeviceUiState {
    data object Loading : EditDeviceUiState

    data object NotFound : EditDeviceUiState

    data class Success(
        val device: Device,
        val deviceTypes: List<DeviceType>,
    ) : EditDeviceUiState
}
