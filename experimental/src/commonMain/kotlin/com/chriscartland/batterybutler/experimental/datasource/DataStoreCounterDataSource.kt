package com.chriscartland.batterybutler.experimental.datasource

import androidx.datastore.core.DataStore
import androidx.datastore.preferences.core.Preferences
import androidx.datastore.preferences.core.edit
import androidx.datastore.preferences.core.longPreferencesKey
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.flow.map
import me.tatarka.inject.annotations.Inject

@Inject
class DataStoreCounterDataSource(
    private val dataStore: DataStore<Preferences>,
) : LocalCounterDataSource {

    private companion object {
        val COUNTER_KEY = longPreferencesKey("experimental_counter")
    }

    override fun observeCounter(): Flow<Long> = dataStore.data
        .map { preferences -> preferences[COUNTER_KEY] ?: 0L }

    override suspend fun getCounter(): Long =
        dataStore.data.first()[COUNTER_KEY] ?: 0L

    override suspend fun incrementCounter(): Long {
        val prefs = dataStore.edit { preferences ->
            val current = preferences[COUNTER_KEY] ?: 0L
            preferences[COUNTER_KEY] = current + 1
        }
        return prefs[COUNTER_KEY] ?: 0L
    }
}
