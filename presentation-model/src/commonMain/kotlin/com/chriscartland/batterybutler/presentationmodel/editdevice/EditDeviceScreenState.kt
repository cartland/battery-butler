package com.chriscartland.batterybutler.presentationmodel.editdevice

import com.chriscartland.batterybutler.domain.model.Device
import com.chriscartland.batterybutler.domain.model.DeviceType

sealed interface EditDeviceScreenState {
    data object Loading : EditDeviceScreenState

    data object NotFound : EditDeviceScreenState

    data class Success(
        val device: Device,
        val deviceTypes: List<DeviceType>,
    ) : EditDeviceScreenState
}
