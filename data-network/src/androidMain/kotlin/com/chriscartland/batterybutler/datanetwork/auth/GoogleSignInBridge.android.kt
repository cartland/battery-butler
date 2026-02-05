package com.chriscartland.batterybutler.datanetwork.auth

import android.app.Activity
import androidx.credentials.ClearCredentialStateRequest
import androidx.credentials.CredentialManager
import androidx.credentials.CustomCredential
import androidx.credentials.GetCredentialRequest
import androidx.credentials.GetCredentialResponse
import androidx.credentials.exceptions.GetCredentialCancellationException
import androidx.credentials.exceptions.GetCredentialException
import androidx.credentials.exceptions.NoCredentialException
import com.chriscartland.batterybutler.domain.model.AuthError
import com.chriscartland.batterybutler.domain.model.Result
import com.google.android.libraries.identity.googleid.GetGoogleIdOption
import com.google.android.libraries.identity.googleid.GoogleIdTokenCredential

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
    private var activityProvider: (() -> Activity)? = null
    private var credentialManager: CredentialManager? = null

    /**
     * Initializes the bridge with the web client ID and activity provider.
     * Must be called before [signIn].
     *
     * @param webClientId The OAuth 2.0 Web Client ID from Google Cloud Console.
     * @param activityProvider Lambda that returns the current Activity for UI prompts.
     */
    fun initialize(
        webClientId: String?,
        activityProvider: () -> Activity,
    ) {
        this.webClientId = webClientId
        this.activityProvider = activityProvider
        activityProvider().let { activity ->
            this.credentialManager = CredentialManager.create(activity)
        }
    }

    actual suspend fun signIn(): Result<GoogleIdToken, AuthError.SignIn> {
        val clientId = webClientId
        if (clientId.isNullOrBlank()) {
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

        val manager = credentialManager
        if (manager == null) {
            return Result.Error(
                AuthError.SignIn.Failed(
                    message = "Sign-in not ready",
                    cause = "CredentialManager not initialized",
                ),
            )
        }

        val googleIdOption = GetGoogleIdOption
            .Builder()
            .setFilterByAuthorizedAccounts(false)
            .setServerClientId(clientId)
            .setAutoSelectEnabled(true)
            .build()

        val request = GetCredentialRequest
            .Builder()
            .addCredentialOption(googleIdOption)
            .build()

        return try {
            val response = manager.getCredential(activity, request)
            handleSignInResponse(response)
        } catch (e: GetCredentialCancellationException) {
            Result.Error(
                AuthError.SignIn.Cancelled(
                    message = "Sign-in cancelled",
                    cause = e.message,
                ),
            )
        } catch (e: NoCredentialException) {
            Result.Error(
                AuthError.SignIn.Failed(
                    message = "No Google account found",
                    cause = e.message,
                ),
            )
        } catch (e: GetCredentialException) {
            val errorMessage = when {
                e.message?.contains("network", ignoreCase = true) == true -> "Network error"
                else -> "Sign-in failed"
            }
            val isNetworkError = e.message?.contains("network", ignoreCase = true) == true
            if (isNetworkError) {
                Result.Error(
                    AuthError.SignIn.NetworkError(
                        message = errorMessage,
                        cause = e.message,
                    ),
                )
            } else {
                Result.Error(
                    AuthError.SignIn.Failed(
                        message = errorMessage,
                        cause = e.message,
                    ),
                )
            }
        }
    }

    private fun handleSignInResponse(
        response: GetCredentialResponse,
    ): Result<GoogleIdToken, AuthError.SignIn> {
        val credential = response.credential

        return when {
            credential is CustomCredential &&
                credential.type == GoogleIdTokenCredential.TYPE_GOOGLE_ID_TOKEN_CREDENTIAL -> {
                val googleIdTokenCredential = GoogleIdTokenCredential.createFrom(credential.data)
                Result.Success(
                    GoogleIdToken(
                        idToken = googleIdTokenCredential.idToken,
                        email = googleIdTokenCredential.id,
                        displayName = googleIdTokenCredential.displayName,
                        photoUrl = googleIdTokenCredential.profilePictureUri?.toString(),
                    ),
                )
            }
            else -> {
                Result.Error(
                    AuthError.SignIn.Failed(
                        message = "Unexpected credential type",
                        cause = "Received ${credential.type} instead of Google ID token",
                    ),
                )
            }
        }
    }

    actual suspend fun signOut() {
        val manager = credentialManager ?: return
        try {
            manager.clearCredentialState(ClearCredentialStateRequest())
        } catch (e: Exception) {
            // Sign out errors are not critical; ignore
        }
    }

    actual fun isConfigured(): Boolean = !webClientId.isNullOrBlank()
}
