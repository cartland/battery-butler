package com.chriscartland.batterybutler.domain.model

data class DeviceInput(
    val name: String,
    val location: String?,
    val typeId: String,
    val imagePath: String? = null,
)
