package com.chriscartland.batterybutler.datanetwork.auth

import com.chriscartland.batterybutler.domain.model.AuthError
import com.chriscartland.batterybutler.domain.model.Result

/**
 * Token received from Google Sign-In.
 *
 * @property idToken The Google ID token (JWT) to be verified by the server.
 * @property email User's email from the Google account.
 * @property displayName User's display name from the Google account.
 * @property photoUrl URL to user's profile photo.
 */
data class GoogleIdToken(
    val idToken: String,
    val email: String?,
    val displayName: String?,
    val photoUrl: String?,
)

/**
 * Platform-specific bridge for Google Sign-In.
 *
 * Each platform implements this using its native Google Sign-In SDK:
 * - Android: Credential Manager API
 * - iOS: Google Sign-In SDK via Swift interop
 * - Desktop: OAuth PKCE flow with browser redirect
 */
expect class GoogleSignInBridge {
    /**
     * Initiates the Google Sign-In flow and returns the ID token.
     *
     * @return [Result.Success] with [GoogleIdToken] on success,
     *         [Result.Error] with [AuthError.SignIn] on failure.
     */
    suspend fun signIn(): Result<GoogleIdToken, AuthError.SignIn>

    /**
     * Signs out the current Google account.
     */
    suspend fun signOut()

    /**
     * Returns true if Google Sign-In is configured for this build.
     */
    fun isConfigured(): Boolean
}
