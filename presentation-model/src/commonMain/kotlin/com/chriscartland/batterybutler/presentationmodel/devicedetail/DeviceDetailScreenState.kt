package com.chriscartland.batterybutler.presentationmodel.devicedetail

import com.chriscartland.batterybutler.domain.model.BatteryEvent
import com.chriscartland.batterybutler.domain.model.Device
import com.chriscartland.batterybutler.domain.model.DeviceType

sealed interface DeviceDetailScreenState {
    data object Loading : DeviceDetailScreenState

    data object NotFound : DeviceDetailScreenState

    data class Success(
        val device: Device,
        val deviceType: DeviceType?,
        val events: List<BatteryEvent>,
    ) : DeviceDetailScreenState
}
