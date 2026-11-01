package com.chriscartland.batterybutler.datalocal

import kotlinx.coroutines.flow.Flow
import kotlin.time.Instant

/**
 * Local-only record of which devices the user has marked as needing a new battery.
 *
 * Kept out of [LocalDataSource] on purpose: everything there round-trips through sync, and this
 * state deliberately does not. See
 * [com.chriscartland.batterybutler.datalocal.room.entity.NeedsBatteryFlagEntity] for why it is a
 * separate table rather than a column on `devices`.
 */
interface NeedsBatteryStore {
    /**
     * Every current mark, keyed by device id, valued by when the mark was made.
     *
     * Carries the timestamp rather than just the ids because the device list's "Recent" sort
     * ranks a device by its most recent activity, and being marked is one such activity.
     * Emits again on every change.
     */
    fun observeFlags(): Flow<Map<String, Instant>>

    /** Marks [deviceId] as needing a battery, recording [flaggedAt] as the moment it was marked. */
    suspend fun flag(
        deviceId: String,
        flaggedAt: Long,
    )

    /** Removes [deviceId]'s mark. A no-op if it was not marked. */
    suspend fun clear(deviceId: String)

    /** Drops every mark -- used when local data is cleared (sign-out, database reset). */
    suspend fun clearAll()
}
