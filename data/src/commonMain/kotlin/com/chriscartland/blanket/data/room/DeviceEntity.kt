package com.chriscartland.blanket.data.room

import androidx.room.Entity
import androidx.room.PrimaryKey
import com.chriscartland.blanket.domain.model.Device
import kotlinx.datetime.Instant

@Entity(tableName = "devices")
data class DeviceEntity(
    @PrimaryKey val id: String,
    val name: String,
    val typeId: String,
    val batteryLastReplaced: Long,
    val lastUpdated: Long, // Store as Long (ms)
    val location: String?,
    val imagePath: String?,
)

fun DeviceEntity.toDomain(): Device =
    Device(
        id = id,
        name = name,
        typeId = typeId,
        batteryLastReplaced = Instant.fromEpochMilliseconds(batteryLastReplaced),
        lastUpdated = Instant.fromEpochMilliseconds(lastUpdated),
        location = location,
        imagePath = imagePath,
    )

fun Device.toEntity(): DeviceEntity =
    DeviceEntity(
        id = id,
        name = name,
        typeId = typeId,
        batteryLastReplaced = batteryLastReplaced.toEpochMilliseconds(),
        lastUpdated = lastUpdated.toEpochMilliseconds(),
        location = location,
        imagePath = imagePath,
    )
