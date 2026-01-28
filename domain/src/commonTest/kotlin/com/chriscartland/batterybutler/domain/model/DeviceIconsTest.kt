package com.chriscartland.batterybutler.domain.model

import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertFalse
import kotlin.test.assertTrue

class DeviceIconsTest {
    @Test
    fun `AvailableIcons is not empty`() {
        assertTrue(DeviceIcons.AvailableIcons.isNotEmpty())
    }

    @Test
    fun `AvailableIcons contains expected shape icons`() {
        val shapes = listOf("star", "circle", "square", "favorite", "diamond", "hexagon")
        for (icon in shapes) {
            assertTrue(
                DeviceIcons.AvailableIcons.contains(icon),
                "Expected shape icon '$icon' to be present",
            )
        }
    }

    @Test
    fun `AvailableIcons contains expected electronic icons`() {
        val electronics = listOf(
            "smartphone",
            "tablet",
            "laptop",
            "watch",
            "headphones",
            "camera",
            "speaker",
            "tv",
            "router",
        )
        for (icon in electronics) {
            assertTrue(
                DeviceIcons.AvailableIcons.contains(icon),
                "Expected electronic icon '$icon' to be present",
            )
        }
    }

    @Test
    fun `AvailableIcons contains expected home icons`() {
        val home = listOf("lightbulb", "detector_smoke", "thermostat", "sensors", "lock")
        for (icon in home) {
            assertTrue(
                DeviceIcons.AvailableIcons.contains(icon),
                "Expected home icon '$icon' to be present",
            )
        }
    }

    @Test
    fun `AvailableIcons contains expected tool icons`() {
        val tools = listOf("flashlight_on", "drill", "brush", "scale")
        for (icon in tools) {
            assertTrue(
                DeviceIcons.AvailableIcons.contains(icon),
                "Expected tool icon '$icon' to be present",
            )
        }
    }

    @Test
    fun `AvailableIcons has no duplicates`() {
        val iconSet = DeviceIcons.AvailableIcons.toSet()
        assertEquals(
            DeviceIcons.AvailableIcons.size,
            iconSet.size,
            "AvailableIcons should not contain duplicates",
        )
    }

    @Test
    fun `AvailableIcons entries are not blank`() {
        for (icon in DeviceIcons.AvailableIcons) {
            assertFalse(
                icon.isBlank(),
                "Icon names should not be blank",
            )
        }
    }

    @Test
    fun `AvailableIcons count matches expected number`() {
        // This test documents the current count and will fail if icons are
        // added or removed, prompting a review of the change
        assertEquals(40, DeviceIcons.AvailableIcons.size)
    }
}
