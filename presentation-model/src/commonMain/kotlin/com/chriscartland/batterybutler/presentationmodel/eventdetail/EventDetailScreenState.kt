package com.chriscartland.batterybutler.presentationmodel.eventdetail

import com.chriscartland.batterybutler.domain.model.BatteryEvent
import com.chriscartland.batterybutler.domain.model.Device
import com.chriscartland.batterybutler.domain.model.DeviceType

sealed interface EventDetailScreenState {
    data object Loading : EventDetailScreenState

    data object NotFound : EventDetailScreenState

    data class Success(
        val event: BatteryEvent,
        val device: Device?,
        val deviceType: DeviceType?,
    ) : EventDetailScreenState
}
