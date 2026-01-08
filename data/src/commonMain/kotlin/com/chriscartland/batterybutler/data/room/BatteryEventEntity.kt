package com.chriscartland.batterybutler.data.room

import androidx.room.Entity
import androidx.room.PrimaryKey
import com.chriscartland.batterybutler.domain.model.BatteryEvent
import kotlinx.datetime.Instant

@Entity(tableName = "battery_events")
data class BatteryEventEntity(
    @PrimaryKey val id: String,
    val deviceId: String,
    val date: Long, // Epoch milliseconds
    val batteryType: String? = null,
    val notes: String? = null,
)

fun BatteryEventEntity.toDomain(): BatteryEvent =
    BatteryEvent(
        id = id,
        deviceId = deviceId,
        date = Instant.fromEpochMilliseconds(date),
        batteryType = batteryType,
        notes = notes,
    )

fun BatteryEvent.toEntity(): BatteryEventEntity =
    BatteryEventEntity(
        id = id,
        deviceId = deviceId,
        date = date.toEpochMilliseconds(),
        batteryType = batteryType,
        notes = notes,
    )
