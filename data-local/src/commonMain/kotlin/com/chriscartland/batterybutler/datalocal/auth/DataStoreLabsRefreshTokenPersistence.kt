package com.chriscartland.batterybutler.datalocal.auth

import androidx.datastore.core.DataStore
import androidx.datastore.preferences.core.Preferences
import androidx.datastore.preferences.core.edit
import androidx.datastore.preferences.core.stringPreferencesKey
import com.chriscartland.batterybutler.domain.repository.LabsRefreshTokenPersistence
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.flow.map
import me.tatarka.inject.annotations.Inject

/**
 * DataStore-backed implementation of [LabsRefreshTokenPersistence], namespaced per
 * [environmentKey] like its [DataStoreLabsSessionStorage] sibling (same shared DataStore, separate
 * key prefix).
 */
@Inject
class DataStoreLabsRefreshTokenPersistence(
    private val dataStore: DataStore<Preferences>,
) : LabsRefreshTokenPersistence {
    private fun refreshTokenKey(environmentKey: String) = stringPreferencesKey("labs_session_${environmentKey}_refresh_token")

    override suspend fun get(environmentKey: String): String? = dataStore.data.map { it[refreshTokenKey(environmentKey)] }.first()

    override suspend fun save(
        environmentKey: String,
        refreshToken: String,
    ) {
        dataStore.edit { it[refreshTokenKey(environmentKey)] = refreshToken }
    }

    override suspend fun clear(environmentKey: String) {
        dataStore.edit { it.remove(refreshTokenKey(environmentKey)) }
    }
}
