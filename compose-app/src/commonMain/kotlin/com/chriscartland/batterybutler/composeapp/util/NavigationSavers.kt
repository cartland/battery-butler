package com.chriscartland.batterybutler.composeapp.util

import androidx.compose.runtime.mutableStateListOf
import androidx.compose.runtime.saveable.listSaver
import androidx.compose.runtime.snapshots.SnapshotStateList
import com.chriscartland.batterybutler.composeapp.Screen
import kotlinx.serialization.encodeToString
import kotlinx.serialization.json.Json

val ScreenListSaver = listSaver<SnapshotStateList<Screen>, String>(
    save = { stateList ->
        val screens = stateList.filterIsInstance<Screen>()
        listOf(Json.encodeToString(screens))
    },
    restore = { restoredList ->
        try {
            val jsonString = restoredList.first()
            val list: List<Screen> = Json.decodeFromString(jsonString)
            val snapshotList = mutableStateListOf<Screen>()
            snapshotList.addAll(list)
            snapshotList
        } catch (_: Exception) {
            mutableStateListOf(Screen.Devices)
        }
    },
)
