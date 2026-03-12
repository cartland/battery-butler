package com.chriscartland.batterybutler.experimental.composeapp.util

import androidx.compose.runtime.mutableStateListOf
import androidx.compose.runtime.saveable.listSaver
import androidx.compose.runtime.snapshots.SnapshotStateList
import com.chriscartland.batterybutler.experimental.composeapp.navigation.ExperimentalScreen
import kotlinx.serialization.encodeToString
import kotlinx.serialization.json.Json

val ExperimentalScreenListSaver = listSaver<SnapshotStateList<ExperimentalScreen>, String>(
    save = { stateList ->
        val screens = stateList.filterIsInstance<ExperimentalScreen>()
        listOf(Json.encodeToString(screens))
    },
    restore = { restoredList ->
        try {
            val jsonString = restoredList.first()
            val list: List<ExperimentalScreen> = Json.decodeFromString(jsonString)
            val snapshotList = mutableStateListOf<ExperimentalScreen>()
            snapshotList.addAll(list)
            snapshotList
        } catch (_: Exception) {
            mutableStateListOf(ExperimentalScreen.Home)
        }
    },
)
