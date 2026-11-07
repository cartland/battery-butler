package com.chriscartland.batterybutler.datalocal.room.entity

import androidx.room.Entity

/**
 * One id from the last full snapshot applied to this database. See
 * [com.chriscartland.batterybutler.datalocal.SnapshotIds] for what the set is for.
 *
 * Lives in the same database as the rows it describes, so a mode switch swaps it along with them
 * and it can never describe another environment's data.
 */
@Entity(tableName = "last_snapshot_ids", primaryKeys = ["kind", "id"])
data class LastSnapshotIdEntity(
    val kind: String,
    val id: String,
) {
    companion object {
        const val KIND_DEVICE_TYPE = "device_type"
        const val KIND_DEVICE = "device"
        const val KIND_EVENT = "event"
    }
}
