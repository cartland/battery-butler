package com.chriscartland.batterybutler.datalocal.auth

import kotlinx.coroutines.flow.Flow

/**
 * Stored authentication token data.
 *
 * @property accessToken Token used for API authentication.
 * @property refreshToken Token used to refresh the access token.
 * @property expiresAtMs Unix timestamp when the access token expires.
 * @property userId Unique identifier for the authenticated user.
 * @property email User's email address.
 * @property displayName User's display name.
 * @property photoUrl URL to user's profile photo.
 */
data class StoredAuthToken(
    val accessToken: String,
    val refreshToken: String?,
    val expiresAtMs: Long,
    val userId: String,
    val email: String?,
    val displayName: String?,
    val photoUrl: String?,
)

/**
 * Interface for persisting authentication tokens.
 *
 * Implementations should use secure storage mechanisms:
 * - Android: EncryptedSharedPreferences or DataStore with encryption
 * - iOS: Keychain
 * - Desktop: Secure credential storage
 */
interface AuthTokenStorage {
    /**
     * Flow of the currently stored token, or null if no token is stored.
     */
    val storedToken: Flow<StoredAuthToken?>

    /**
     * Saves the authentication token.
     */
    suspend fun saveToken(token: StoredAuthToken)

    /**
     * Clears the stored token.
     */
    suspend fun clearToken()
}
