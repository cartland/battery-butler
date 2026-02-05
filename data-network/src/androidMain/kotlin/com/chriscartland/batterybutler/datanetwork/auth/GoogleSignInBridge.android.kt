package com.chriscartland.batterybutler.datanetwork.auth

import com.chriscartland.batterybutler.domain.model.AuthError
import com.chriscartland.batterybutler.domain.model.Result

/**
 * Android implementation of [GoogleSignInBridge].
 *
 * Uses Android Credential Manager API for Google Sign-In.
 * Requires initialization with Activity context before use.
 *
 * Configuration:
 * - Set `GOOGLE_WEB_CLIENT_ID` in local.properties
 * - Configure OAuth consent screen in Google Cloud Console
 * - Add SHA-1 fingerprint for the app in Google Cloud Console
 */
actual class GoogleSignInBridge {
    private var webClientId: String? = null
    private var activityProvider: (() -> android.app.Activity)? = null

    /**
     * Initializes the bridge with the web client ID and activity provider.
     * Must be called before [signIn].
     *
     * @param webClientId The OAuth 2.0 Web Client ID from Google Cloud Console.
     * @param activityProvider Lambda that returns the current Activity for UI prompts.
     */
    fun initialize(
        webClientId: String?,
        activityProvider: () -> android.app.Activity,
    ) {
        this.webClientId = webClientId
        this.activityProvider = activityProvider
    }

    actual suspend fun signIn(): Result<GoogleIdToken, AuthError.SignIn> {
        val clientId = webClientId
        if (clientId.isNullOrBlank()) {
            // OAuth not configured - return a clear error
            return Result.Error(
                AuthError.SignIn.Failed(
                    message = "Google Sign-In not configured",
                    cause = "GOOGLE_WEB_CLIENT_ID is not set in local.properties",
                ),
            )
        }

        val activity = activityProvider?.invoke()
        if (activity == null) {
            return Result.Error(
                AuthError.SignIn.Failed(
                    message = "Cannot show sign-in",
                    cause = "Activity not available",
                ),
            )
        }

        // TODO: Implement actual Credential Manager flow
        // For now, return a placeholder indicating the feature is not yet implemented
        return Result.Error(
            AuthError.SignIn.Failed(
                message = "Google Sign-In not yet implemented",
                cause = "Credential Manager integration pending",
            ),
        )
    }

    actual suspend fun signOut() {
        // TODO: Implement Credential Manager sign-out
    }

    actual fun isConfigured(): Boolean = !webClientId.isNullOrBlank()
}
