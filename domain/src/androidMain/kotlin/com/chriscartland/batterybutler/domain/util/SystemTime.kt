package com.chriscartland.batterybutler.domain.util

import kotlinx.datetime.Clock
import kotlinx.datetime.Instant

@OptIn(kotlin.time.ExperimentalTime::class)
actual object SystemTime {
    actual fun now(): Instant = Clock.System.now()
}
