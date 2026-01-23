package com.chriscartland.batterybutler.composeapp.util

import androidx.compose.runtime.mutableStateListOf
import androidx.compose.runtime.saveable.SaverScope
import com.chriscartland.batterybutler.composeapp.Screen
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertTrue
import kotlin.text.contains

class NavigationSaversTest {
    private val dummySaverScope = object : SaverScope {
        override fun canBeSaved(value: Any): Boolean = true
    }

    @Test
    fun testSaveAndRestore() {
        // Prepare original list
        val originalList = mutableStateListOf<Screen>(
            Screen.Devices,
            Screen.DeviceDetail("device_123"),
            Screen.Settings,
        )

        // Save
        val saved = with(ScreenListSaver) {
            dummySaverScope.save(originalList)
        } as List<String>

        // Verify saved JSON structure (optional, but good for regression)
        val jsonString = saved[0]
        assertTrue("Devices" in jsonString)
        assertTrue("device_123" in jsonString)

        // Restore
        val restoredList = ScreenListSaver.restore(saved)!!

        // Verify restoration
        assertEquals(3, restoredList.size)
        assertEquals(Screen.Devices, restoredList[0])
        assertEquals(Screen.DeviceDetail("device_123"), restoredList[1])
        assertEquals(Screen.Settings, restoredList[2])
    }

    @Test
    fun testRestoreInvalidJson() {
        // Simulate corrupted save state
        val corruptedSave = listOf("invalid_json_string")

        // Restore should default to [Screen.Devices]
        val restoredList = ScreenListSaver.restore(corruptedSave as List<Any>)!!

        assertEquals(1, restoredList.size)
        assertEquals(Screen.Devices, restoredList[0])
    }
}
