package com.chriscartland.batterybutler.domain.util

import kotlinx.datetime.Instant

@OptIn(kotlin.time.ExperimentalTime::class)
expect object SystemTime {
    fun now(): Instant
}
