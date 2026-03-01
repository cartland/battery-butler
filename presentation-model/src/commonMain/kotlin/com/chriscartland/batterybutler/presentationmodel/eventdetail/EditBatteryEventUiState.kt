package com.chriscartland.batterybutler.presentationmodel.eventdetail

import com.chriscartland.batterybutler.domain.model.BatteryEvent
import com.chriscartland.batterybutler.domain.model.Device
import com.chriscartland.batterybutler.domain.model.DeviceType

sealed interface EditBatteryEventUiState {
    data object Loading : EditBatteryEventUiState

    data object NotFound : EditBatteryEventUiState

    data class Success(
        val event: BatteryEvent,
        val device: Device?,
        val deviceType: DeviceType?,
    ) : EditBatteryEventUiState
}
