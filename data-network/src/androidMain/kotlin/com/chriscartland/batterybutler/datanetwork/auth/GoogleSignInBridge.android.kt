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
import com.google.android.libraries.identity.googleid.GetSignInWithGoogleOption
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
    private var dispatcherProvider: com.chriscartland.batterybutler.domain.model.DispatcherProvider? = null

    /**
     * Initialize with OAuth client ID. Call from Application.onCreate().
     *
     * @param webClientId The OAuth 2.0 Web Client ID from Google Cloud Console.
     * @param dispatcherProvider Injected dispatchers.
     */
    actual fun initialize(
        clientId: String?,
        dispatcherProvider: com.chriscartland.batterybutler.domain.model.DispatcherProvider?,
    ) {
        this.webClientId = clientId
        this.dispatcherProvider = dispatcherProvider
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
            Log.e(TAG, "Failed to create CredentialManager: ${e.message ?: "Unknown error"}")
        }
    }

    actual fun unbindActivity() {
        this.activityProvider = null
        this.credentialManager = null
        Log.i(TAG, "CredentialManager and Activity unbound")
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
            Log.i(TAG, "Google Sign-In: Configured with client ID ...${webClientId.orEmpty().takeLast(15)}")
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
        return performSignIn(clientId)
    }

    actual suspend fun signInSilently(): Result<GoogleIdToken, AuthError.SignIn> {
        val clientId = webClientId
        if (clientId.isNullOrBlank()) {
            return Result.Error(
                AuthError.SignIn.Failed(
                    message = "Google Sign-In not configured",
                    cause = "GOOGLE_WEB_CLIENT_ID is not set in local.properties",
                ),
            )
        }
        return performSignIn(clientId, filterByAuthorizedAccounts = true)
    }

    actual suspend fun signInWithClient(
        clientId: String,
        clientSecret: String?,
    ): Result<GoogleIdToken, AuthError.SignIn> {
        if (!clientSecret.isNullOrBlank()) {
            // Credential Manager authenticates with the server client ID only; the OAuth client
            // secret (used by desktop "Desktop app" clients at token exchange) has no role here.
            Log.d(TAG, "signInWithClient: clientSecret provided but ignored on Android (Credential Manager)")
        }
        if (clientId.isBlank()) {
            return Result.Error(
                AuthError.SignIn.Failed(
                    message = "Google Sign-In not configured",
                    cause = "Client ID is blank",
                ),
            )
        }
        return performSignIn(clientId)
    }

    actual suspend fun signInSilentlyWithClient(
        clientId: String,
        clientSecret: String?,
    ): Result<GoogleIdToken, AuthError.SignIn> {
        if (clientId.isBlank()) {
            return Result.Error(
                AuthError.SignIn.Failed(
                    message = "Google Sign-In not configured",
                    cause = "Client ID is blank",
                ),
            )
        }
        return performSignIn(clientId, filterByAuthorizedAccounts = true)
    }

    private suspend fun performSignIn(
        clientId: String,
        filterByAuthorizedAccounts: Boolean = false,
    ): Result<GoogleIdToken, AuthError.SignIn> {
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
            .setFilterByAuthorizedAccounts(filterByAuthorizedAccounts)
            .setServerClientId(clientId)
            // Auto-select only makes sense on the silent (authorized-accounts) path. On an explicit
            // interactive button press, auto-select is what makes the request throw
            // NoCredentialException instead of showing the account picker, so gate it on the flag.
            .setAutoSelectEnabled(filterByAuthorizedAccounts)
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
            if (filterByAuthorizedAccounts) {
                // Expected/common outcome for a silent, authorized-accounts-only request (e.g. the
                // very first sign-in ever, before any account has been authorized) -- not a
                // misconfiguration signal, so no warning-level log here. Callers swallow this.
                Log.i(TAG, "Silent sign-in: no already-authorized account for client ...${clientId.takeLast(15)}")
                Result.Error(
                    AuthError.SignIn.Failed(
                        message = "No Google account found",
                        cause = e.message,
                    ),
                )
            } else {
                // Interactive path: the authorized-accounts Google ID request found nothing to show,
                // so retry ONCE with the explicit "Sign in with Google" (add-account) flow, which
                // always launches interactive UI instead of failing when there's no auto-selectable
                // credential. Only if that also fails do we surface an error to the user.
                Log.i(
                    TAG,
                    "Interactive sign-in: no auto-selectable Google account for client " +
                        "...${clientId.takeLast(15)}; retrying with the explicit Sign in with Google flow",
                )
                retryWithSignInWithGoogle(activity, manager, clientId)
            }
        } catch (e: GetCredentialException) {
            Log.w(
                TAG,
                "GetCredentialException (${e.type}) for client ...${clientId.takeLast(15)}: ${e.message.orEmpty()}",
            )
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

    /**
     * Interactive fallback for when the authorized-accounts Google ID request throws
     * [NoCredentialException] (no auto-selectable credential). [GetSignInWithGoogleOption] is the
     * explicit "Sign in with Google" / add-account flow: it always launches interactive UI, so it
     * shows the account picker (or add-account screen) instead of failing. Routed through the same
     * [handleSignInResponse]. If it *still* throws [NoCredentialException]/[GetCredentialException],
     * we surface a user-facing error -- with clearer copy than the raw "No credentials available".
     */
    private suspend fun retryWithSignInWithGoogle(
        activity: Activity,
        manager: CredentialManager,
        clientId: String,
    ): Result<GoogleIdToken, AuthError.SignIn> {
        val signInWithGoogleOption = GetSignInWithGoogleOption
            .Builder(clientId)
            .build()
        val request = GetCredentialRequest
            .Builder()
            .addCredentialOption(signInWithGoogleOption)
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
            Log.w(
                TAG,
                "NoCredentialException on the Sign in with Google retry for client " +
                    "...${clientId.takeLast(15)} — if a Google account exists on-device, this usually " +
                    "means the app's signing certificate (e.g. the Play App Signing SHA-1, which " +
                    "differs from the upload keystore's) isn't registered on the Android OAuth client " +
                    "for this project. Cause: ${e.message.orEmpty()}",
            )
            Result.Error(
                AuthError.SignIn.Failed(
                    message = "No Google account found",
                    cause = "No Google account is available on this device, or this app build " +
                        "isn't registered for sign-in.",
                ),
            )
        } catch (e: GetCredentialException) {
            Log.w(
                TAG,
                "GetCredentialException (${e.type}) on the Sign in with Google retry for client " +
                    "...${clientId.takeLast(15)}: ${e.message.orEmpty()}",
            )
            val isNetworkError = e.message?.contains("network", ignoreCase = true) == true
            if (isNetworkError) {
                Result.Error(
                    AuthError.SignIn.NetworkError(
                        message = "Network error",
                        cause = e.message,
                    ),
                )
            } else {
                Result.Error(
                    AuthError.SignIn.Failed(
                        message = "Sign-in failed",
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

    @Suppress("SwallowedException")
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
