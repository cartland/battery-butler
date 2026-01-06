package com.chriscartland.blanket.domain.model

import kotlinx.datetime.Instant

data class BatteryEvent(
    val id: String,
    val deviceId: String,
    val date: Instant,
)
