package com.chriscartland.batterybutler.presentationcore.util

import kotlinx.datetime.LocalDateTime

fun generateFileTimestamp(now: LocalDateTime): String {
    val year = now.year
    val month = (now.month.ordinal + 1).toString().padStart(2, '0')
    val day = now.day.toString().padStart(2, '0')
    val hour = now.hour.toString().padStart(2, '0')
    val minute = now.minute.toString().padStart(2, '0')
    val second = now.second.toString().padStart(2, '0')
    return "${year}_${month}_${day}_${hour}_${minute}_$second"
}
