package com.chriscartland.batterybutler.domain.model

import kotlinx.datetime.Instant

data class Device(
    val id: String,
    val name: String,
    val typeId: String,
    val batteryLastReplaced: Instant,
    val lastUpdated: Instant,
    val location: String? = null,
    val imagePath: String? = null,
)
