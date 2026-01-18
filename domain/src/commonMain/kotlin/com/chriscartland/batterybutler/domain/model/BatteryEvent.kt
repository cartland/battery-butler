package com.chriscartland.batterybutler.domain.model

import kotlinx.datetime.Instant

@OptIn(kotlin.time.ExperimentalTime::class)
data class BatteryEvent(
    val id: String,
    val deviceId: String,
    val date: Instant,
    val batteryType: String? = null,
    val notes: String? = null,
)
