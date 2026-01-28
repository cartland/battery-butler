package com.chriscartland.batterybutler.domain.model

import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertIs
import kotlin.test.assertNotEquals
import kotlin.test.assertTrue

class AppVersionTest {
    @Test
    fun `Unavailable is singleton instance`() {
        val unavailable1 = AppVersion.Unavailable
        val unavailable2 = AppVersion.Unavailable

        assertEquals(unavailable1, unavailable2)
    }

    @Test
    fun `Android version stores versionName and versionCode`() {
        val version = AppVersion.Android(versionName = "1.2.3", versionCode = 123)

        assertEquals("1.2.3", version.versionName)
        assertEquals(123, version.versionCode)
    }

    @Test
    fun `Android versions with same values are equal`() {
        val version1 = AppVersion.Android(versionName = "1.0.0", versionCode = 1)
        val version2 = AppVersion.Android(versionName = "1.0.0", versionCode = 1)

        assertEquals(version1, version2)
        assertEquals(version1.hashCode(), version2.hashCode())
    }

    @Test
    fun `Android versions with different values are not equal`() {
        val version1 = AppVersion.Android(versionName = "1.0.0", versionCode = 1)
        val version2 = AppVersion.Android(versionName = "1.0.1", versionCode = 2)

        assertNotEquals(version1, version2)
    }

    @Test
    fun `Ios version stores versionName and buildNumber`() {
        val version = AppVersion.Ios(versionName = "2.0.0", buildNumber = "456")

        assertEquals("2.0.0", version.versionName)
        assertEquals("456", version.buildNumber)
    }

    @Test
    fun `Ios versions with same values are equal`() {
        val version1 = AppVersion.Ios(versionName = "2.0.0", buildNumber = "100")
        val version2 = AppVersion.Ios(versionName = "2.0.0", buildNumber = "100")

        assertEquals(version1, version2)
        assertEquals(version1.hashCode(), version2.hashCode())
    }

    @Test
    fun `Desktop version stores versionName`() {
        val version = AppVersion.Desktop(versionName = "3.0.0")

        assertEquals("3.0.0", version.versionName)
    }

    @Test
    fun `Desktop versions with same values are equal`() {
        val version1 = AppVersion.Desktop(versionName = "3.0.0")
        val version2 = AppVersion.Desktop(versionName = "3.0.0")

        assertEquals(version1, version2)
        assertEquals(version1.hashCode(), version2.hashCode())
    }

    @Test
    fun `sealed class variants are distinguishable`() {
        val unavailable: AppVersion = AppVersion.Unavailable
        val android: AppVersion = AppVersion.Android("1.0", 1)
        val ios: AppVersion = AppVersion.Ios("1.0", "1")
        val desktop: AppVersion = AppVersion.Desktop("1.0")

        assertIs<AppVersion.Unavailable>(unavailable)
        assertIs<AppVersion.Android>(android)
        assertIs<AppVersion.Ios>(ios)
        assertIs<AppVersion.Desktop>(desktop)
    }

    @Test
    fun `when expression covers all variants`() {
        val versions = listOf(
            AppVersion.Unavailable,
            AppVersion.Android("1.0", 1),
            AppVersion.Ios("1.0", "1"),
            AppVersion.Desktop("1.0"),
        )

        for (version in versions) {
            val result = when (version) {
                is AppVersion.Unavailable -> "unavailable"
                is AppVersion.Android -> "android"
                is AppVersion.Ios -> "ios"
                is AppVersion.Desktop -> "desktop"
            }
            // If we get here without error, the when is exhaustive
            assertTrue(result.isNotEmpty())
        }
    }
}
