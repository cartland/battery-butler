package com.chriscartland.batterybutler.datalocal.preferences

import androidx.datastore.core.DataStore
import androidx.datastore.preferences.core.Preferences
import androidx.datastore.preferences.core.edit
import androidx.datastore.preferences.core.stringPreferencesKey
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.map
import me.tatarka.inject.annotations.Inject

/**
 * DataStore-backed implementation of [PreferencesDataSource].
 */
@Inject
class DataStorePreferencesDataSource(
    private val dataStore: DataStore<Preferences>,
) : PreferencesDataSource {
    private companion object {
        // Storage key literal deliberately left as "network_mode" (not renamed to match the
        // DataMode rename) so existing installs keep their saved selection across the app update
        // that ships this rename, instead of silently reverting to the default.
        val DATA_MODE_KEY = stringPreferencesKey("network_mode")
    }

    override val dataModeValue: Flow<String?> = dataStore.data
        .map { preferences -> preferences[DATA_MODE_KEY] }

    override suspend fun setDataModeValue(value: String) {
        dataStore.edit { preferences ->
            preferences[DATA_MODE_KEY] = value
        }
    }
}
