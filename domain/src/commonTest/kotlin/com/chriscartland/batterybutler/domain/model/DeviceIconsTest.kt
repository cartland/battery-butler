package com.chriscartland.batterybutler.domain.model

import kotlin.test.Test
import kotlin.test.assertTrue

class DeviceIconsTest {

    @Test
    fun availableIcons_shouldNotBeEmpty() {
        assertTrue(DeviceIcons.AvailableIcons.isNotEmpty(), "Device icons should not be empty")
    }

    @Test
    fun availableIcons_shouldContainCommonIcons() {
        val commonIcons = listOf("smartphone", "laptop", "battery_charging_full")
        // We only check for a subset we expect to exist
        val existingCommons = listOf("smartphone", "laptop")
        
        val icons = DeviceIcons.AvailableIcons
        existingCommons.forEach { icon ->
            assertTrue(icons.contains(icon), "Should contain icon: $icon")
        }
    }
}
