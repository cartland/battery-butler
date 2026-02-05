package com.chriscartland.batterybutler.datanetwork.auth

import com.chriscartland.batterybutler.domain.model.AuthError
import com.chriscartland.batterybutler.domain.model.Result

/**
 * Desktop (JVM) implementation of [GoogleSignInBridge].
 *
 * Uses OAuth 2.0 PKCE flow with browser redirect for desktop applications.
 */
actual class GoogleSignInBridge {
    private var clientId: String? = null

    /**
     * Initializes the bridge with the client ID.
     *
     * @param clientId The OAuth 2.0 Client ID from Google Cloud Console.
     */
    fun initialize(clientId: String?) {
        this.clientId = clientId
    }

    actual suspend fun signIn(): Result<GoogleIdToken, AuthError.SignIn> {
        if (clientId.isNullOrBlank()) {
            return Result.Error(
                AuthError.SignIn.Failed(
                    message = "Google Sign-In not configured",
                    cause = "Client ID is not configured",
                ),
            )
        }

        // TODO: Implement OAuth PKCE flow with browser redirect
        return Result.Error(
            AuthError.SignIn.Failed(
                message = "Google Sign-In not yet implemented",
                cause = "Desktop OAuth PKCE flow integration pending",
            ),
        )
    }

    actual suspend fun signOut() {
        // TODO: Clear stored tokens
    }

    actual fun isConfigured(): Boolean = !clientId.isNullOrBlank()
}
