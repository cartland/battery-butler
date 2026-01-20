package com.chriscartland.batterybutler.presentationcore.util

import kotlinx.datetime.LocalDateTime
import kotlin.test.Test
import kotlin.test.assertEquals

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
