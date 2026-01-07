package com.chriscartland.blanket.feature.devicetypes

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.chriscartland.blanket.domain.model.DeviceType
import com.chriscartland.blanket.domain.repository.DeviceRepository
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.launch
import me.tatarka.inject.annotations.Inject

@Inject
class EditDeviceTypeViewModelFactory(
    private val deviceRepository: DeviceRepository,
) {
    fun create(typeId: String): EditDeviceTypeViewModel = EditDeviceTypeViewModel(typeId, deviceRepository)
}

class EditDeviceTypeViewModel(
    private val typeId: String,
    private val deviceRepository: DeviceRepository,
) : ViewModel() {
    val uiState: StateFlow<EditDeviceTypeUiState> = deviceRepository
        .getDeviceTypeById(typeId)
        .map { type ->
            if (type == null) {
                EditDeviceTypeUiState.NotFound
            } else {
                EditDeviceTypeUiState.Success(type)
            }
        }.stateIn(
            scope = viewModelScope,
            started = SharingStarted.WhileSubscribed(5000),
            initialValue = EditDeviceTypeUiState.Loading,
        )

    fun updateDeviceType(
        name: String,
        batteryType: String,
        batteryQuantity: Int,
        defaultIcon: String,
    ) {
        val currentState = uiState.value
        if (currentState is EditDeviceTypeUiState.Success) {
            viewModelScope.launch {
                val updatedType = currentState.deviceType.copy(
                    name = name,
                    batteryType = batteryType,
                    batteryQuantity = batteryQuantity,
                    defaultIcon = defaultIcon,
                )
                deviceRepository.updateDeviceType(updatedType)
            }
        }
    }

    fun deleteDeviceType() {
        viewModelScope.launch {
            deviceRepository.deleteDeviceType(typeId)
        }
    }
}

sealed interface EditDeviceTypeUiState {
    data object Loading : EditDeviceTypeUiState

    data object NotFound : EditDeviceTypeUiState

    data class Success(
        val deviceType: DeviceType,
    ) : EditDeviceTypeUiState
}
