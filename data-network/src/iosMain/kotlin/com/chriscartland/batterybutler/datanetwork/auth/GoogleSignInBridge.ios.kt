package com.chriscartland.batterybutler.datanetwork.auth

import com.chriscartland.batterybutler.domain.model.AuthError
import com.chriscartland.batterybutler.domain.model.Result

/**
 * iOS implementation of [GoogleSignInBridge].
 *
 * Uses Google Sign-In SDK via Swift interop.
 * Requires configuration in Info.plist with `GIDClientID`.
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
        if (clientId.isNullOrBlank()) {
            println("Google Sign-In (iOS): Not configured - add GIDClientID to Info.plist")
        } else {
            println("Google Sign-In (iOS): Configured with client ID ...${clientId.takeLast(15)}")
        }
    }

    actual suspend fun signIn(): Result<GoogleIdToken, AuthError.SignIn> {
        if (clientId.isNullOrBlank()) {
            return Result.Error(
                AuthError.SignIn.Failed(
                    message = "Google Sign-In not configured",
                    cause = "GIDClientID is not set in Info.plist",
                ),
            )
        }

        // TODO: Implement Google Sign-In SDK integration via Swift interop
        return Result.Error(
            AuthError.SignIn.Failed(
                message = "Google Sign-In not yet implemented",
                cause = "iOS Google Sign-In SDK integration pending",
            ),
        )
    }

    actual suspend fun signOut() {
        // TODO: Implement Google Sign-In SDK sign-out
    }

    actual fun isConfigured(): Boolean = !clientId.isNullOrBlank()
}
