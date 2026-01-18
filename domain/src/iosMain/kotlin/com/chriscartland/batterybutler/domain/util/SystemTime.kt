package com.chriscartland.batterybutler.domain.util

import kotlinx.datetime.Instant
import platform.Foundation.NSDate
import platform.Foundation.timeIntervalSince1970

@OptIn(kotlin.time.ExperimentalTime::class)
actual object SystemTime {
    actual fun now(): Instant {
        val secondsDouble = NSDate().timeIntervalSince1970
        val seconds = secondsDouble.toLong()
        val nanoseconds = ((secondsDouble - seconds) * 1_000_000_000).toInt()
        return Instant.fromEpochSeconds(seconds, nanoseconds)
    }
}
