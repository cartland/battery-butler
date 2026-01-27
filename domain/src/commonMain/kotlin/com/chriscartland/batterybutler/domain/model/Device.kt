package com.chriscartland.batterybutler.domain.model

import kotlin.time.Instant

@OptIn(kotlin.time.ExperimentalTime::class)
data class Device(
    val id: String,
    val name: String,
    val typeId: String,
    val batteryLastReplaced: Instant,
    val lastUpdated: Instant,
    val location: String? = null,
    val imagePath: String? = null,
) {
    init {
        require(id.isNotBlank()) { "Device ID cannot be blank" }
        require(name.isNotBlank()) { "Device name cannot be blank" }
    }
}
