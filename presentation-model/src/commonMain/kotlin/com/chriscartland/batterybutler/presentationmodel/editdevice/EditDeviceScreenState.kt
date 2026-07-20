package com.chriscartland.batterybutler.presentationmodel.editdevice

import com.chriscartland.batterybutler.domain.model.Device
import com.chriscartland.batterybutler.domain.model.DeviceImageBytes
import com.chriscartland.batterybutler.domain.model.DeviceType

sealed interface EditDeviceScreenState {
    data object Loading : EditDeviceScreenState

    data object NotFound : EditDeviceScreenState

    data class Success(
        val device: Device,
        val deviceTypes: List<DeviceType>,
        /** True only when the current backend supports device images (Labs modes). */
        val imagesSupported: Boolean = false,
        /** Null when [device] has no photo, or it hasn't finished caching yet -- show the fallback icon either way. */
        val imageBytes: DeviceImageBytes? = null,
    ) : EditDeviceScreenState
}
