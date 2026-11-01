package com.chriscartland.batterybutler.presentationcore.util

import kotlinx.datetime.FixedOffsetTimeZone
import kotlinx.datetime.LocalDateTime
import kotlinx.datetime.LocalTime
import kotlinx.datetime.TimeZone
import kotlinx.datetime.UtcOffset
import kotlinx.datetime.atStartOfDayIn
import kotlinx.datetime.toLocalDateTime
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.time.Instant

class DateTimeUtilsTest {
    @Test
    fun testGenerateFileTimestamp() {
        val date = LocalDateTime(
            year = 2023,
            monthNumber = 10,
            dayOfMonth = 5,
            hour = 14,
            minute = 30,
            second = 45,
            nanosecond = 0,
        )
        // Expected format: yyyy_MM_dd_HH_mm_ss
        // 2023_10_05_14_30_45
        val expected = "2023_10_05_14_30_45"
        val actual = generateFileTimestamp(date)
        assertEquals(expected, actual)
    }

    @Test
    fun testGenerateFileTimestampSingleDigits() {
        val date = LocalDateTime(
            year = 2024,
            monthNumber = 1,
            dayOfMonth = 9,
            hour = 3,
            minute = 5,
            second = 7,
            nanosecond = 0,
        )
        // Expected format: yyyy_MM_dd_HH_mm_ss
        // 2024_01_09_03_05_07
        // Month 1 (January) -> ordinal 0 -> ordinal + 1 = 1 -> "01"
        val expected = "2024_01_09_03_05_07"
        val actual = generateFileTimestamp(date)
        assertEquals(expected, actual)
    }
}

/**
 * Regression tests for the DatePicker/instant timezone boundary.
 *
 * Material3 reports a picked day as UTC midnight; battery events are instants read back in
 * the user's zone. Getting the conversion wrong shifted the saved date by a day west of UTC
 * and the opened date by a day east of UTC.
 */
class DatePickerTimeZoneTest {
    private val losAngeles = FixedOffsetTimeZone(UtcOffset(hours = -7))
    private val tokyo = FixedOffsetTimeZone(UtcOffset(hours = 9))
    private val midnight = LocalTime(0, 0)

    private fun localDateOf(
        instant: Instant,
        timeZone: TimeZone,
    ): String = instant.toLocalDateTime(timeZone).date.toString()

    @Test
    fun pickedDaySurvivesRoundTripWestOfUtc() {
        // The picker reports 2026-09-06 as UTC midnight.
        val picked = Instant.parse("2026-09-06T00:00:00Z").toEpochMilliseconds()

        val saved = datePickerMillisToInstant(picked, losAngeles, midnight)

        // Before the fix this instant read back as 2026-09-05 in Los Angeles.
        assertEquals("2026-09-06", localDateOf(saved, losAngeles))
        assertEquals(picked, instantToDatePickerMillis(saved, losAngeles))
    }

    @Test
    fun pickedDaySurvivesRoundTripEastOfUtc() {
        val picked = Instant.parse("2026-09-06T00:00:00Z").toEpochMilliseconds()

        val saved = datePickerMillisToInstant(picked, tokyo, midnight)

        assertEquals("2026-09-06", localDateOf(saved, tokyo))
        assertEquals(picked, instantToDatePickerMillis(saved, tokyo))
    }

    @Test
    fun localStartOfDayOpensPickerOnSameDayEastOfUtc() {
        // How the add-event screen stores 2026-09-06 in Tokyo: 2026-09-05T15:00Z.
        val stored = kotlinx.datetime.LocalDate
            .parse("2026-09-06")
            .atStartOfDayIn(tokyo)

        val pickerMillis = instantToDatePickerMillis(stored, tokyo)

        // Before the fix the raw instant was handed to the picker, which read it as 09-05.
        assertEquals("2026-09-06T00:00:00Z", Instant.fromEpochMilliseconds(pickerMillis).toString())
        assertEquals("2026-09-06", formatDatePickerMillis(pickerMillis))
    }

    @Test
    fun localStartOfDayOpensPickerOnSameDayWestOfUtc() {
        val stored = kotlinx.datetime.LocalDate
            .parse("2026-09-06")
            .atStartOfDayIn(losAngeles)

        val pickerMillis = instantToDatePickerMillis(stored, losAngeles)

        assertEquals("2026-09-06T00:00:00Z", Instant.fromEpochMilliseconds(pickerMillis).toString())
    }

    @Test
    fun reconfirmingTheDisplayedDateDoesNotChangeTheInstant() {
        // Opening the editor, tapping the same day, and saving must be a no-op.
        val stored = Instant.parse("2026-09-06T14:32:00Z")
        val timeOfDay = stored.toLocalDateTime(losAngeles).time

        val pickerMillis = instantToDatePickerMillis(stored, losAngeles)
        val saved = datePickerMillisToInstant(pickerMillis, losAngeles, timeOfDay)

        assertEquals(stored, saved)
    }

    @Test
    fun changingTheDayKeepsTheTimeOfDay() {
        val stored = Instant.parse("2026-09-06T14:32:00Z") // 07:32 in Los Angeles
        val timeOfDay = stored.toLocalDateTime(losAngeles).time
        val nextDay = Instant.parse("2026-09-07T00:00:00Z").toEpochMilliseconds()

        val saved = datePickerMillisToInstant(nextDay, losAngeles, timeOfDay)

        assertEquals("2026-09-07", localDateOf(saved, losAngeles))
        assertEquals(timeOfDay, saved.toLocalDateTime(losAngeles).time)
    }

    @Test
    fun formatsPickerSelectionInUtcRegardlessOfHostZone() {
        val picked = Instant.parse("2026-01-01T00:00:00Z").toEpochMilliseconds()

        assertEquals("2026-01-01", formatDatePickerMillis(picked))
    }
}
