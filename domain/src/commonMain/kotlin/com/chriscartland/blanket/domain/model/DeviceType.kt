package com.chriscartland.blanket.domain.model

data class DeviceType(
    val id: String,
    val name: String,
    // Material Icon name or similar identifier
    val defaultIcon: String? = null,
    val batteryType: String = "AA",
    val batteryQuantity: Int = 1,
)
