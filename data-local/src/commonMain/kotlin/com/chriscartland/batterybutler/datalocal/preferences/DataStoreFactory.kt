package com.chriscartland.batterybutler.datalocal.preferences

import androidx.datastore.core.DataStore
import androidx.datastore.preferences.core.Preferences

/**
 * Factory for creating DataStore instances.
 * Platform-specific implementations provide the appropriate file path.
 */
expect class DataStoreFactory {
    fun createPreferencesDataStore(): DataStore<Preferences>
}

internal const val PREFERENCES_FILE_NAME = "battery_butler_preferences.preferences_pb"
