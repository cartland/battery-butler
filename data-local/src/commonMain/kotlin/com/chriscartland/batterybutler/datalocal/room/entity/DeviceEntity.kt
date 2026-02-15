package com.chriscartland.batterybutler.datalocal.room.entity

import androidx.room.Entity
import androidx.room.PrimaryKey
import com.chriscartland.batterybutler.domain.model.Device
import kotlinx.datetime.Clock
import kotlin.time.Instant

@OptIn(kotlin.time.ExperimentalTime::class)
@Entity(tableName = "devices")
data class DeviceEntity(
    @PrimaryKey val id: String,
    val name: String,
    val typeId: String,
    val batteryLastReplaced: Long,
    val lastUpdated: Long, // Store as Long (ms)
    val location: String?,
    val imagePath: String?,
    val isSynced: Boolean = false,
    val isDeleted: Boolean = false,
    val lastModified: Long = Clock.System.now().toEpochMilliseconds(),
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

fun Device.toEntity(
    isSynced: Boolean = false,
    isDeleted: Boolean = false,
    lastModified: Long = Clock.System.now().toEpochMilliseconds(),
): DeviceEntity =
    DeviceEntity(
        id = id,
        name = name,
        typeId = typeId,
        batteryLastReplaced = batteryLastReplaced.toEpochMilliseconds(),
        lastUpdated = lastUpdated.toEpochMilliseconds(),
        location = location,
        imagePath = imagePath,
        isSynced = isSynced,
        isDeleted = isDeleted,
        lastModified = lastModified,
    )
