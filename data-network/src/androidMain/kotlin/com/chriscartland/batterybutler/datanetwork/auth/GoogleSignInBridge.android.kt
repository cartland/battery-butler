package com.chriscartland.batterybutler.datanetwork.auth

import android.app.Activity
import android.util.Log
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
     * Initialize with OAuth client ID. Call from Application.onCreate().
     *
     * @param webClientId The OAuth 2.0 Web Client ID from Google Cloud Console.
     */
    fun initialize(webClientId: String?) {
        this.webClientId = webClientId
        logConfiguration()
    }

    /**
     * Bind activity for CredentialManager. Call from MainActivity.onCreate().
     *
     * @param activityProvider Lambda that returns the current Activity for UI prompts.
     */
    fun bindActivity(activityProvider: () -> Activity) {
        this.activityProvider = activityProvider
        try {
            this.credentialManager = CredentialManager.create(activityProvider())
            Log.i(TAG, "CredentialManager initialized successfully")
        } catch (e: Exception) {
            Log.e(TAG, "Failed to create CredentialManager: ${e.message}")
        }
    }

    private fun logConfiguration() {
        if (webClientId.isNullOrBlank()) {
            Log.w(TAG, "┌─────────────────────────────────────────────────────────────")
            Log.w(TAG, "│ Google Sign-In: NOT CONFIGURED")
            Log.w(TAG, "├─────────────────────────────────────────────────────────────")
            Log.w(TAG, "│ Sign-in will show 'Coming Soon' message to users.")
            Log.w(TAG, "│")
            Log.w(TAG, "│ To enable:")
            Log.w(TAG, "│   1. Get Web Client ID from Google Cloud Console")
            Log.w(TAG, "│   2. Add to local.properties:")
            Log.w(TAG, "│      GOOGLE_WEB_CLIENT_ID=your-id.apps.googleusercontent.com")
            Log.w(TAG, "│   3. Register app's SHA-1 fingerprint:")
            Log.w(TAG, "│      ./gradlew signingReport")
            Log.w(TAG, "└─────────────────────────────────────────────────────────────")
        } else {
            Log.i(TAG, "Google Sign-In: Configured with client ID ...${webClientId?.takeLast(15)}")
        }
    }

    companion object {
        private const val TAG = "GoogleSignInBridge"
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
