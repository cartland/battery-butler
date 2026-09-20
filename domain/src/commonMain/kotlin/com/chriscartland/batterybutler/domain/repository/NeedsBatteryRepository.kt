package com.chriscartland.batterybutler.domain.repository

import com.chriscartland.batterybutler.domain.model.DataError
import com.chriscartland.batterybutler.domain.model.Result
import kotlinx.coroutines.flow.Flow

/**
 * Tracks which devices the user has manually marked as needing a new battery.
 *
 * This is a to-do list, not history: a mark says "this one still needs doing". Recording a battery
 * replacement clears it (see `AddBatteryEventUseCase`), so the mark never contradicts the device's
 * own `batteryLastReplaced`.
 *
 * Separate from [DeviceRepository] because the mark is local-only -- it is not part of the sync
 * wire format and does not travel between a user's devices.
 */
interface NeedsBatteryRepository {
    /** The ids of every device currently marked. Emits again whenever a mark is added or removed. */
    fun getFlaggedDeviceIds(): Flow<Set<String>>

    /** Adds or removes [deviceId]'s mark. Setting a mark that already exists just refreshes its timestamp. */
    suspend fun setNeedsBattery(
        deviceId: String,
        needsBattery: Boolean,
    ): Result<Unit, DataError>

    /** Drops every mark. Called when local data is cleared. */
    suspend fun clearAll(): Result<Unit, DataError>
}
