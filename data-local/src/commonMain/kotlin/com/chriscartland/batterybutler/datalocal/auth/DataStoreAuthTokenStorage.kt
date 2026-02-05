package com.chriscartland.batterybutler.datalocal.auth

import androidx.datastore.core.DataStore
import androidx.datastore.preferences.core.Preferences
import androidx.datastore.preferences.core.edit
import androidx.datastore.preferences.core.longPreferencesKey
import androidx.datastore.preferences.core.stringPreferencesKey
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.map
import me.tatarka.inject.annotations.Inject

/**
 * DataStore-backed implementation of [AuthTokenStorage].
 *
 * Note: For production, consider using encrypted storage:
 * - Android: EncryptedSharedPreferences
 * - iOS: Keychain via expect/actual
 */
@Inject
class DataStoreAuthTokenStorage(
    private val dataStore: DataStore<Preferences>,
) : AuthTokenStorage {
    private companion object {
        val ACCESS_TOKEN_KEY = stringPreferencesKey("auth_access_token")
        val REFRESH_TOKEN_KEY = stringPreferencesKey("auth_refresh_token")
        val EXPIRES_AT_KEY = longPreferencesKey("auth_expires_at")
        val USER_ID_KEY = stringPreferencesKey("auth_user_id")
        val EMAIL_KEY = stringPreferencesKey("auth_email")
        val DISPLAY_NAME_KEY = stringPreferencesKey("auth_display_name")
        val PHOTO_URL_KEY = stringPreferencesKey("auth_photo_url")
    }

    override val storedToken: Flow<StoredAuthToken?> = dataStore.data
        .map { preferences ->
            val accessToken = preferences[ACCESS_TOKEN_KEY] ?: return@map null
            val userId = preferences[USER_ID_KEY] ?: return@map null
            val expiresAt = preferences[EXPIRES_AT_KEY] ?: return@map null

            StoredAuthToken(
                accessToken = accessToken,
                refreshToken = preferences[REFRESH_TOKEN_KEY],
                expiresAtMs = expiresAt,
                userId = userId,
                email = preferences[EMAIL_KEY],
                displayName = preferences[DISPLAY_NAME_KEY],
                photoUrl = preferences[PHOTO_URL_KEY],
            )
        }

    override suspend fun saveToken(token: StoredAuthToken) {
        dataStore.edit { preferences ->
            preferences[ACCESS_TOKEN_KEY] = token.accessToken
            preferences[EXPIRES_AT_KEY] = token.expiresAtMs
            preferences[USER_ID_KEY] = token.userId

            if (token.refreshToken != null) {
                preferences[REFRESH_TOKEN_KEY] = token.refreshToken
            } else {
                preferences.remove(REFRESH_TOKEN_KEY)
            }

            if (token.email != null) {
                preferences[EMAIL_KEY] = token.email
            } else {
                preferences.remove(EMAIL_KEY)
            }

            if (token.displayName != null) {
                preferences[DISPLAY_NAME_KEY] = token.displayName
            } else {
                preferences.remove(DISPLAY_NAME_KEY)
            }

            if (token.photoUrl != null) {
                preferences[PHOTO_URL_KEY] = token.photoUrl
            } else {
                preferences.remove(PHOTO_URL_KEY)
            }
        }
    }

    override suspend fun clearToken() {
        dataStore.edit { preferences ->
            preferences.remove(ACCESS_TOKEN_KEY)
            preferences.remove(REFRESH_TOKEN_KEY)
            preferences.remove(EXPIRES_AT_KEY)
            preferences.remove(USER_ID_KEY)
            preferences.remove(EMAIL_KEY)
            preferences.remove(DISPLAY_NAME_KEY)
            preferences.remove(PHOTO_URL_KEY)
        }
    }
}
