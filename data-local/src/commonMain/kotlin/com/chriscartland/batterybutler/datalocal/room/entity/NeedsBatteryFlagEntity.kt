package com.chriscartland.batterybutler.datalocal.room.entity

import androidx.room.Entity
import androidx.room.PrimaryKey

/**
 * A user's manual "this device needs a new battery" mark.
 *
 * Deliberately its own table rather than a column on `devices`: device rows are overwritten
 * wholesale by sync (`insertDevices` uses [androidx.room.OnConflictStrategy.REPLACE], and a full
 * snapshot rewrites every row), so a column here would be silently cleared on the next sync. The
 * flag is local-only and never leaves the device -- the wire format has no field for it.
 *
 * Presence in this table *is* the flag; there is no boolean column. [flaggedAt] records when the
 * mark was made so the UI can say how long a device has been waiting.
 */
@Entity(tableName = "needs_battery_flags")
data class NeedsBatteryFlagEntity(
    @PrimaryKey val deviceId: String,
    val flaggedAt: Long,
)
