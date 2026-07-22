package com.chriscartland.batterybutler.presentationmodel.devicedetail

import com.chriscartland.batterybutler.domain.model.BatteryEvent
import com.chriscartland.batterybutler.domain.model.Device
import com.chriscartland.batterybutler.domain.model.DeviceImageBytes
import com.chriscartland.batterybutler.domain.model.DeviceType

sealed interface DeviceDetailScreenState {
    data object Loading : DeviceDetailScreenState

    data object NotFound : DeviceDetailScreenState

    data class Success(
        val device: Device,
        val deviceType: DeviceType?,
        val events: List<BatteryEvent>,
        /** Null when [device] has no photo, or it hasn't finished caching yet -- show the fallback icon either way. */
        val imageBytes: DeviceImageBytes? = null,
    ) : DeviceDetailScreenState

    /**
     * The state chain threw (e.g. a transient Room/SQLite failure racing the sync loop's
     * snapshot upsert). Rendered with a Retry affordance -- without this variant the screen
     * used to wedge at [Loading] forever when the upstream flow terminated.
     */
    data class Error(
        val message: String,
    ) : DeviceDetailScreenState
}
