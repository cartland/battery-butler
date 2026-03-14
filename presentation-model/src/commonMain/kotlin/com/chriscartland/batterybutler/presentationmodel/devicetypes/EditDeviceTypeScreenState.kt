package com.chriscartland.batterybutler.presentationmodel.devicetypes

import com.chriscartland.batterybutler.domain.model.DeviceType

sealed interface EditDeviceTypeScreenState {
    data object Loading : EditDeviceTypeScreenState

    data object NotFound : EditDeviceTypeScreenState

    data class Success(
        val deviceType: DeviceType,
        val usedIcons: List<String>,
    ) : EditDeviceTypeScreenState
}
