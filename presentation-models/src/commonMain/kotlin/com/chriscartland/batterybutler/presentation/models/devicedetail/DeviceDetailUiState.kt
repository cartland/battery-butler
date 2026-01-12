package com.chriscartland.batterybutler.presentation.models.devicedetail

import com.chriscartland.batterybutler.domain.model.BatteryEvent
import com.chriscartland.batterybutler.domain.model.Device
import com.chriscartland.batterybutler.domain.model.DeviceType

sealed interface DeviceDetailUiState {
    data object Loading : DeviceDetailUiState

    data object NotFound : DeviceDetailUiState

    data class Success(
        val device: Device,
        val deviceType: DeviceType?,
        val events: List<BatteryEvent>,
    ) : DeviceDetailUiState
}
