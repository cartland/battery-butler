package com.chriscartland.batterybutler.presentationmodel.devicetypes

import com.chriscartland.batterybutler.domain.model.Device
import com.chriscartland.batterybutler.domain.model.DeviceType

sealed interface DeviceTypeDetailScreenState {
    data object Loading : DeviceTypeDetailScreenState

    data object NotFound : DeviceTypeDetailScreenState

    data class Success(
        val deviceType: DeviceType,
        val devices: List<Device>,
    ) : DeviceTypeDetailScreenState
}
