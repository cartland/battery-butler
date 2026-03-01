package com.chriscartland.batterybutler.presentationmodel.devicetypes

import com.chriscartland.batterybutler.domain.model.Device
import com.chriscartland.batterybutler.domain.model.DeviceType

sealed interface DeviceTypeDetailUiState {
    data object Loading : DeviceTypeDetailUiState

    data object NotFound : DeviceTypeDetailUiState

    data class Success(
        val deviceType: DeviceType,
        val devices: List<Device>,
    ) : DeviceTypeDetailUiState
}
