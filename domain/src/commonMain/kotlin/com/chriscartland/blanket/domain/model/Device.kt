package com.chriscartland.blanket.domain.model

import kotlinx.datetime.Instant

data class Device(
    val id: String,
    val name: String,
    val typeId: String,
    // 0.0 to 1.0
    val batteryLevel: Float,
    val batteryLastReplaced: Instant,
    val lastUpdated: Instant,
    val imagePath: String? = null,
)
