package com.chriscartland.blanket.data.room

import androidx.room.Entity
import androidx.room.PrimaryKey
import com.chriscartland.blanket.domain.model.DeviceType

@Entity(tableName = "device_types")
data class DeviceTypeEntity(
    @PrimaryKey val id: String,
    val name: String,
    val defaultIcon: String?,
    val batteryType: String = "AA",
    val batteryQuantity: Int = 1,
)

fun DeviceTypeEntity.toDomain(): DeviceType =
    DeviceType(
        id = id,
        name = name,
        defaultIcon = defaultIcon,
        batteryType = batteryType,
        batteryQuantity = batteryQuantity,
    )

fun DeviceType.toEntity(): DeviceTypeEntity =
    DeviceTypeEntity(
        id = id,
        name = name,
        defaultIcon = defaultIcon,
        batteryType = batteryType,
        batteryQuantity = batteryQuantity,
    )
