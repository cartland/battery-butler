package com.chriscartland.blanket.domain.model

import kotlinx.datetime.Instant

data class Device(
    val id: String,
    val name: String,
    val typeId: String,
    val batteryLastReplaced: Instant,
    val lastUpdated: Instant,
    val imagePath: String? = null,
)
