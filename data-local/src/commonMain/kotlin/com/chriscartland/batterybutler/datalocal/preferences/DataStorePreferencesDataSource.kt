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
        val NETWORK_MODE_KEY = stringPreferencesKey("network_mode")
    }

    override val networkModeValue: Flow<String?> = dataStore.data
        .map { preferences -> preferences[NETWORK_MODE_KEY] }

    override suspend fun setNetworkModeValue(value: String) {
        dataStore.edit { preferences ->
            preferences[NETWORK_MODE_KEY] = value
        }
    }
}
