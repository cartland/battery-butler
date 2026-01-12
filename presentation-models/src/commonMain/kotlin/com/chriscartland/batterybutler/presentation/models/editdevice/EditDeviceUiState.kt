package com.chriscartland.batterybutler.presentation.models.editdevice

import com.chriscartland.batterybutler.domain.model.Device
import com.chriscartland.batterybutler.domain.model.DeviceType

sealed interface EditDeviceUiState {
    data object Loading : EditDeviceUiState
    data object NotFound : EditDeviceUiState
    data class Success(
        val device: Device,
        val deviceTypes: List<DeviceType>,
    ) : EditDeviceUiState
}
