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
     * Like [signIn], but never shows any UI: succeeds only if the platform can silently confirm an
     * already-authorized account for the client passed to [initialize], and fails immediately
     * otherwise. Used to opportunistically renew the own-backend session (see
     * [com.chriscartland.batterybutler.data.repository.auth.DefaultAuthRepository]) without
     * prompting the user for their Google account again after the local session token expires.
     *
     * Platforms with no such capability (iOS, Desktop -- both use an interactive OAuth sheet/browser
     * with no persisted, silently-restorable session) always return [Result.Error] immediately.
     *
     * @return [Result.Success] with [GoogleIdToken] on success,
     *         [Result.Error] with [AuthError.SignIn] when no silent credential is available.
     */
    suspend fun signInSilently(): Result<GoogleIdToken, AuthError.SignIn>

    /**
     * Like [signIn], but signs in against an EXPLICIT OAuth client rather than the one passed to
     * [initialize]. Used for Labs sign-in, which needs a Google ID token minted for the Labs OAuth
     * client (whose audience the Labs Firebase project trusts).
     *
     * @param clientId the OAuth client ID to authenticate against.
     * @param clientSecret the OAuth client secret. Google "Desktop app" clients require it at the
     *   token-exchange step (it is not confidential for installed apps); platforms that don't use a
     *   secret (Android Credential Manager, iOS) ignore it. Pass null when not applicable.
     * @return [Result.Success] with [GoogleIdToken] on success,
     *         [Result.Error] with [AuthError.SignIn] on failure.
     */
    suspend fun signInWithClient(
        clientId: String,
        clientSecret: String?,
    ): Result<GoogleIdToken, AuthError.SignIn>

    /**
     * Like [signInWithClient], but never shows any UI: succeeds only if the platform can silently
     * confirm an already-authorized account for [clientId] (e.g. Android Credential Manager's
     * authorized-accounts-only flow), and fails immediately otherwise. Used to opportunistically
     * re-establish a real session after a process restart, when a persisted "believed signed in"
     * flag already says the user was signed in -- so it must never prompt.
     *
     * Platforms with no such capability (iOS, Desktop -- both use an interactive OAuth sheet/browser
     * with no persisted, silently-restorable session) always return [Result.Error] immediately.
     *
     * @param clientId the OAuth client ID to authenticate against.
     * @param clientSecret same meaning as in [signInWithClient]; pass null when not applicable.
     * @return [Result.Success] with [GoogleIdToken] on success,
     *         [Result.Error] with [AuthError.SignIn] when no silent credential is available.
     */
    suspend fun signInSilentlyWithClient(
        clientId: String,
        clientSecret: String?,
    ): Result<GoogleIdToken, AuthError.SignIn>

    /**
     * Signs out the current Google account.
     */
    suspend fun signOut()

    /**
     * Initializes the bridge with platform-specific parameters.
     *
     * @param clientId The OAuth web or platform client ID.
     * @param dispatcherProvider Injected dispatchers for coroutine execution.
     */
    fun initialize(
        clientId: String?,
        dispatcherProvider: com.chriscartland.batterybutler.domain.model.DispatcherProvider?,
    )

    /**
     * Returns true if Google Sign-In is configured for this build.
     */
    fun isConfigured(): Boolean

    /**
     * Unbinds the activity context. Call from Activity.onDestroy().
     */
    fun unbindActivity()
}
