package com.chriscartland.batterybutler.datalocal.room.entity

import androidx.room.Entity
import androidx.room.PrimaryKey
import com.chriscartland.batterybutler.domain.model.BatteryEvent
import kotlinx.datetime.Clock
import kotlin.time.Instant

@Entity(tableName = "battery_events")
data class BatteryEventEntity(
    @PrimaryKey val id: String,
    val deviceId: String,
    val date: Long, // Epoch milliseconds
    val batteryType: String? = null,
    val notes: String? = null,
    val isSynced: Boolean = false,
    val isDeleted: Boolean = false,
    val lastModified: Long = Clock.System.now().toEpochMilliseconds(),
)

fun BatteryEventEntity.toDomain(): BatteryEvent =
    BatteryEvent(
        id = id,
        deviceId = deviceId,
        date = Instant.fromEpochMilliseconds(date),
        batteryType = batteryType,
        notes = notes,
    )

fun BatteryEvent.toEntity(
    isSynced: Boolean = false,
    isDeleted: Boolean = false,
    lastModified: Long = Clock.System.now().toEpochMilliseconds(),
): BatteryEventEntity =
    BatteryEventEntity(
        id = id,
        deviceId = deviceId,
        date = date.toEpochMilliseconds(),
        batteryType = batteryType,
        notes = notes,
        isSynced = isSynced,
        isDeleted = isDeleted,
        lastModified = lastModified,
    )
