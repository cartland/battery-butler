package com.chriscartland.batterybutler.datalocal.auth

import com.chriscartland.batterybutler.domain.model.User
import kotlinx.coroutines.flow.Flow

/**
 * Persists the belief that the user is signed in to a given Labs environment, keyed by an opaque
 * environment key (the same partition key [com.chriscartland.batterybutler.datanetwork.apiKeyForMode]
 * already uses to key in-memory Labs auth state — staging and prod each get their own slot).
 *
 * This is deliberately lightweight: no tokens, no expiry. It exists purely so the UI-facing
 * [com.chriscartland.batterybutler.domain.model.AuthState] can survive a process restart without
 * showing a "signed out" prompt next to already-cached local data. The real Labs session/token
 * lives only in `LabsAuthGateway` and is not persisted here or restored from here.
 */
interface LabsSessionStorage {
    /** Emits the believed-signed-in user for [environmentKey], or null if not signed in. */
    fun observeUser(environmentKey: String): Flow<User?>

    /** Records that the user is believed signed in to [environmentKey] as [user]. */
    suspend fun saveUser(
        environmentKey: String,
        user: User,
    )

    /** Clears the believed-signed-in user for [environmentKey]. */
    suspend fun clearUser(environmentKey: String)
}
