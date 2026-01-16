package com.chriscartland.batterybutler.presentationmodels.devicetypes

import com.chriscartland.batterybutler.domain.model.DeviceType

sealed interface EditDeviceTypeUiState {
    data object Loading : EditDeviceTypeUiState

    data object NotFound : EditDeviceTypeUiState

    data class Success(
        val deviceType: DeviceType,
    ) : EditDeviceTypeUiState
}
