package com.chriscartland.batterybutler.presentationcore.util

import kotlinx.datetime.LocalDateTime
import kotlinx.datetime.LocalTime
import kotlinx.datetime.TimeZone
import kotlinx.datetime.atStartOfDayIn
import kotlinx.datetime.atTime
import kotlinx.datetime.toInstant
import kotlinx.datetime.toLocalDateTime
import kotlin.time.Instant

fun generateFileTimestamp(now: LocalDateTime): String {
    val year = now.year
    val month = (now.month.ordinal + 1).toString().padStart(2, '0')
    val day = now.day.toString().padStart(2, '0')
    val hour = now.hour.toString().padStart(2, '0')
    val minute = now.minute.toString().padStart(2, '0')
    val second = now.second.toString().padStart(2, '0')
    return "${year}_${month}_${day}_${hour}_${minute}_$second"
}

/**
 * Material3's `DatePickerState.selectedDateMillis` is UTC midnight of the picked calendar
 * day, but battery events are stored as instants read back in the user's own time zone.
 * Handing the picker value straight to the domain shifts the date by a day for every zone
 * with a negative UTC offset; seeding the picker with a raw instant shifts it for every
 * zone with a positive one. These two functions are the boundary between the conventions
 * and are exact inverses at day granularity.
 */
fun instantToDatePickerMillis(
    instant: Instant,
    timeZone: TimeZone,
): Long =
    instant
        .toLocalDateTime(timeZone)
        .date
        .atStartOfDayIn(TimeZone.UTC)
        .toEpochMilliseconds()

/**
 * Converts a [DatePicker][androidx.compose.material3.DatePicker] selection back into an
 * instant, keeping [timeOfDay] so that re-picking the date already shown does not silently
 * rewrite an event's time (iOS creates events at the actual time, not at midnight).
 */
fun datePickerMillisToInstant(
    millis: Long,
    timeZone: TimeZone,
    timeOfDay: LocalTime,
): Instant =
    Instant
        .fromEpochMilliseconds(millis)
        .toLocalDateTime(TimeZone.UTC)
        .date
        .atTime(timeOfDay)
        .toInstant(timeZone)

/** Formats a picker selection for display, reading it in the same UTC convention the picker uses. */
fun formatDatePickerMillis(millis: Long): String =
    Instant
        .fromEpochMilliseconds(millis)
        .toLocalDateTime(TimeZone.UTC)
        .date
        .toString()
