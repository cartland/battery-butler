package com.chriscartland.batterybutler.domain.model

data class DeviceTypeInput(
    val name: String,
    val defaultIcon: String?,
    val batteryType: String,
    val batteryQuantity: Int,
)
