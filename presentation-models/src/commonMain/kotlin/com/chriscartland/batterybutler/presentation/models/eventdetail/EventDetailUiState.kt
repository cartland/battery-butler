package com.chriscartland.batterybutler.presentation.models.eventdetail

import com.chriscartland.batterybutler.domain.model.BatteryEvent
import com.chriscartland.batterybutler.domain.model.Device
import com.chriscartland.batterybutler.domain.model.DeviceType

sealed interface EventDetailUiState {
    data object Loading : EventDetailUiState

    data object NotFound : EventDetailUiState

    data class Success(
        val event: BatteryEvent,
        val device: Device,
        val deviceType: DeviceType?,
    ) : EventDetailUiState
}
