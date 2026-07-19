package com.chriscartland.batterybutler.datalocal.auth

import androidx.datastore.core.DataStore
import androidx.datastore.preferences.core.Preferences
import androidx.datastore.preferences.core.edit
import androidx.datastore.preferences.core.stringPreferencesKey
import com.chriscartland.batterybutler.domain.model.User
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.map
import me.tatarka.inject.annotations.Inject

/**
 * DataStore-backed implementation of [LabsSessionStorage]. Preference keys are namespaced per
 * [environmentKey] so staging and prod each get an independent slot in the same shared DataStore.
 */
@Inject
class DataStoreLabsSessionStorage(
    private val dataStore: DataStore<Preferences>,
) : LabsSessionStorage {
    private fun userIdKey(environmentKey: String) = stringPreferencesKey("labs_session_${environmentKey}_user_id")

    private fun emailKey(environmentKey: String) = stringPreferencesKey("labs_session_${environmentKey}_email")

    private fun displayNameKey(environmentKey: String) = stringPreferencesKey("labs_session_${environmentKey}_display_name")

    private fun photoUrlKey(environmentKey: String) = stringPreferencesKey("labs_session_${environmentKey}_photo_url")

    override fun observeUser(environmentKey: String): Flow<User?> =
        dataStore.data.map { preferences ->
            val userId = preferences[userIdKey(environmentKey)] ?: return@map null
            User(
                id = userId,
                email = preferences[emailKey(environmentKey)],
                displayName = preferences[displayNameKey(environmentKey)],
                photoUrl = preferences[photoUrlKey(environmentKey)],
            )
        }

    override suspend fun saveUser(
        environmentKey: String,
        user: User,
    ) {
        val email = user.email
        val displayName = user.displayName
        val photoUrl = user.photoUrl
        dataStore.edit { preferences ->
            preferences[userIdKey(environmentKey)] = user.id

            if (email != null) {
                preferences[emailKey(environmentKey)] = email
            } else {
                preferences.remove(emailKey(environmentKey))
            }

            if (displayName != null) {
                preferences[displayNameKey(environmentKey)] = displayName
            } else {
                preferences.remove(displayNameKey(environmentKey))
            }

            if (photoUrl != null) {
                preferences[photoUrlKey(environmentKey)] = photoUrl
            } else {
                preferences.remove(photoUrlKey(environmentKey))
            }
        }
    }

    override suspend fun clearUser(environmentKey: String) {
        dataStore.edit { preferences ->
            preferences.remove(userIdKey(environmentKey))
            preferences.remove(emailKey(environmentKey))
            preferences.remove(displayNameKey(environmentKey))
            preferences.remove(photoUrlKey(environmentKey))
        }
    }
}
