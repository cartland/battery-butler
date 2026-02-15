package com.chriscartland.batterybutler.datalocal.room.entity

import androidx.room.Entity
import androidx.room.PrimaryKey
import com.chriscartland.batterybutler.domain.model.DeviceType
import kotlinx.datetime.Clock

@Entity(tableName = "device_types")
data class DeviceTypeEntity(
    @PrimaryKey val id: String,
    val name: String,
    val defaultIcon: String?,
    val batteryType: String = "AA",
    val batteryQuantity: Int = 1,
    val isSynced: Boolean = false,
    val isDeleted: Boolean = false,
    val lastModified: Long = Clock.System.now().toEpochMilliseconds(),
)

fun DeviceTypeEntity.toDomain(): DeviceType =
    DeviceType(
        id = id,
        name = name,
        defaultIcon = defaultIcon,
        batteryType = batteryType,
        batteryQuantity = batteryQuantity,
    )

fun DeviceType.toEntity(
    isSynced: Boolean = false,
    isDeleted: Boolean = false,
    lastModified: Long = Clock.System.now().toEpochMilliseconds(),
): DeviceTypeEntity =
    DeviceTypeEntity(
        id = id,
        name = name,
        defaultIcon = defaultIcon,
        batteryType = batteryType,
        batteryQuantity = batteryQuantity,
        isSynced = isSynced,
        isDeleted = isDeleted,
        lastModified = lastModified,
    )
