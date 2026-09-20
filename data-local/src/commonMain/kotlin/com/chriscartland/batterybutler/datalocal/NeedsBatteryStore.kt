package com.chriscartland.batterybutler.datalocal

import kotlinx.coroutines.flow.Flow

/**
 * Local-only record of which devices the user has marked as needing a new battery.
 *
 * Kept out of [LocalDataSource] on purpose: everything there round-trips through sync, and this
 * state deliberately does not. See
 * [com.chriscartland.batterybutler.datalocal.room.entity.NeedsBatteryFlagEntity] for why it is a
 * separate table rather than a column on `devices`.
 */
interface NeedsBatteryStore {
    /** The ids of every device currently marked as needing a battery. Emits again on every change. */
    fun observeFlaggedDeviceIds(): Flow<Set<String>>

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
