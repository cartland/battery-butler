package com.chriscartland.batterybutler.domain.repository

import com.chriscartland.batterybutler.domain.model.BatteryEvent
import com.chriscartland.batterybutler.domain.model.Device
import com.chriscartland.batterybutler.domain.model.DeviceType

data class RemoteUpdate(
    val isFullSnapshot: Boolean,
    val deviceTypes: List<DeviceType>,
    val devices: List<Device>,
    val events: List<BatteryEvent>,
)
