package com.chriscartland.batterybutler.data.repository.auth

import co.touchlab.kermit.Logger
import com.chriscartland.batterybutler.datalocal.auth.AuthTokenStorage
import com.chriscartland.batterybutler.datalocal.auth.StoredAuthToken
import com.chriscartland.batterybutler.datanetwork.auth.GoogleSignInBridge
import com.chriscartland.batterybutler.domain.model.AuthError
import com.chriscartland.batterybutler.domain.model.AuthState
import com.chriscartland.batterybutler.domain.model.DataMode
import com.chriscartland.batterybutler.domain.model.Result
import com.chriscartland.batterybutler.domain.model.User
import com.chriscartland.batterybutler.domain.repository.AuthRepository
import com.chriscartland.batterybutler.domain.repository.DataModeRepository
import com.chriscartland.batterybutler.proto.AuthServiceClient
import com.chriscartland.batterybutler.proto.VerifyTokenRequest
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Job
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.launch
import me.tatarka.inject.annotations.Inject
import kotlin.coroutines.cancellation.CancellationException
import kotlin.time.Clock
import kotlin.time.ExperimentalTime

/**
 * Default implementation of [AuthRepository].
 *
 * Handles:
 * - Google Sign-In flow via [GoogleSignInBridge]
 * - Server token verification via [AuthServiceClient]
 * - Token persistence via [AuthTokenStorage]
 * - Session state management
 *
 * After Google Sign-In, verifies the ID token with the server to get a
 * session token. Falls back to local-only auth if the server is unreachable.
 *
 * The session token's lifetime is intentionally short (24h if the server verified it,
 * [LOCAL_TOKEN_EXPIRY_MS] -- 1h -- if only locally-verified) since it isn't itself refreshable
 * ([refreshToken] is a stub). Instead of forcing the user through the interactive account picker
 * every time it lapses, both [scheduleTokenExpiry] (fires in-memory) and cold start (a stored
 * token found already expired) drive [attemptSilentRefresh]: one opportunistic, no-UI attempt via
 * [GoogleSignInBridge.signInSilently] to confirm the same Google account is still authorized on
 * this device, re-verified with the server exactly like an explicit sign-in. Success re-arms the
 * next expiry with zero user interaction; failure (no silently-restorable session -- currently
 * only Android's Credential Manager has one) falls back to the original behavior of clearing the
 * token and dropping to [AuthState.Unauthenticated]. See `bb-auth-session-length` in TODO.md.
 */
@OptIn(ExperimentalTime::class)
@Inject
class DefaultAuthRepository(
    private val googleSignInBridge: GoogleSignInBridge,
    private val authServiceClient: AuthServiceClient,
    private val tokenStorage: AuthTokenStorage,
    private val dataModeRepository: DataModeRepository,
    private val scope: CoroutineScope,
) : AuthRepository {
    private val log = Logger.withTag("DefaultAuthRepository")

    private val _authState = MutableStateFlow<AuthState>(AuthState.Unknown)
    override val authState: StateFlow<AuthState> = _authState.asStateFlow()
    private var expiryJob: Job? = null

    @Suppress("ElseCaseInsteadOfExhaustiveWhen")
    override val currentUser: Flow<User?> = authState.map { state ->
        when (state) {
            is AuthState.Authenticated -> state.user
            else -> null // Intentional: unauthenticated states have no user
        }
    }

    init {
        // Check for stored token on initialization
        scope.launch {
            tokenStorage.storedToken.collect { storedToken ->
                if (_authState.value is AuthState.Unknown) {
                    if (storedToken == null) {
                        _authState.value = AuthState.Unauthenticated
                    } else {
                        // Optimistic: show the believed-signed-in user immediately (same rationale
                        // as DefaultLabsAuthRepository's persisted belief) rather than an
                        // already-expired token forcing a signed-out flash before the silent
                        // refresh below has even had a chance to run.
                        _authState.value = storedToken.toAuthState()
                        val now = Clock.System.now().toEpochMilliseconds()
                        if (storedToken.expiresAtMs > now) {
                            scheduleTokenExpiry(storedToken.expiresAtMs)
                        } else {
                            attemptSilentRefresh()
                        }
                    }
                }
            }
        }
    }

    override fun isSignInAvailable(): Boolean = googleSignInBridge.isConfigured()

    override suspend fun signInWithGoogle(): Result<User, AuthError> {
        val currentMode = dataModeRepository.dataMode.first()
        if (currentMode is DataMode.None) {
            log.w { "Data Mode is None (Offline), skipping Google Sign-In" }
            return Result.Error(
                AuthError.SignIn.NetworkError(
                    message = "Offline Mode Enabled",
                    cause = "Data Mode is set to None",
                ),
            )
        }

        if (!googleSignInBridge.isConfigured()) {
            log.w { "Google Sign-In not configured" }
            val error = AuthError.Configuration.NotConfigured(
                message = "Sign-in not available",
                cause = "Google Sign-In is not configured for this build",
            )
            _authState.value = AuthState.Failed(error)
            return Result.Error(error)
        }

        _authState.value = AuthState.Authenticating

        return when (val result = googleSignInBridge.signIn()) {
            is Result.Success -> {
                val googleToken = result.data
                log.i { "Google Sign-In successful for ${googleToken.email}" }

                verifyWithServer(
                    googleToken.idToken,
                    googleToken.email,
                    googleToken.displayName,
                    googleToken.photoUrl,
                    isExplicitAttempt = true,
                )
            }

            is Result.Error -> {
                log.w { "Google Sign-In failed: ${result.error.message}" }
                _authState.value = AuthState.Failed(result.error)
                Result.Error(result.error)
            }
        }
    }

    override suspend fun signOut() {
        log.i { "Signing out" }
        expiryJob?.cancel()
        googleSignInBridge.signOut()
        tokenStorage.clearToken()
        _authState.value = AuthState.Unauthenticated
    }

    override suspend fun refreshToken(): Result<Unit, AuthError> {
        // Session tokens are not refreshable — user must sign in again
        log.w { "Token refresh not supported, user must re-authenticate" }
        return Result.Error(
            AuthError.Token.Expired(
                message = "Session expired",
                cause = "Please sign in again",
            ),
        )
    }

    /**
     * Verify the Google ID token with the server to get a session token.
     * Falls back to local-only auth if the server is unreachable.
     *
     * @param isExplicitAttempt True when called from a user-initiated [signInWithGoogle], false
     *   when called from the background [attemptSilentRefresh]. A server-rejected token only drops
     *   to [AuthState.Failed] (which [LoginContent] auto-shows as an error dialog) for an explicit
     *   attempt the user is actively waiting on -- a background attempt fails quietly to
     *   [AuthState.Unauthenticated] instead, matching how a failed [GoogleSignInBridge.signInSilently]
     *   call is already handled in [attemptSilentRefresh]. Otherwise a routine background refresh
     *   cycle could leave the user with an unprompted error dialog whose only action opens the
     *   interactive picker. See `bb-silent-reauth-cooldown` in TODO.md.
     */
    private suspend fun verifyWithServer(
        idToken: String,
        email: String?,
        displayName: String?,
        photoUrl: String?,
        isExplicitAttempt: Boolean,
    ): Result<User, AuthError> {
        // Check data mode first
        val currentMode = dataModeRepository.dataMode.first()
        if (currentMode is DataMode.None) {
            log.i { "Data Mode None: Skipping server verification, using local-only auth" }
            return fallbackToLocalAuth(idToken, email, displayName, photoUrl)
        }

        return try {
            val response = authServiceClient
                .VerifyToken()
                .execute(VerifyTokenRequest(google_id_token = idToken))

            if (response.valid) {
                log.i { "Server verified token for ${response.email}" }
                val user = User(
                    id = response.user_id,
                    email = response.email,
                    displayName = response.display_name,
                    photoUrl = response.photo_url,
                )
                val storedToken = StoredAuthToken(
                    accessToken = response.session_token,
                    refreshToken = null,
                    expiresAtMs = response.expires_at_ms,
                    userId = user.id,
                    email = user.email,
                    displayName = user.displayName,
                    photoUrl = user.photoUrl,
                )
                tokenStorage.saveToken(storedToken)
                scheduleTokenExpiry(storedToken.expiresAtMs)
                _authState.value = AuthState.Authenticated(user)
                Result.Success(user)
            } else {
                log.w { "Server rejected token: ${response.error_message}" }
                val error = AuthError.Token.Invalid(
                    message = "Server rejected token",
                    cause = response.error_message,
                )
                if (!isExplicitAttempt) {
                    // Background silent-refresh: drop the stale token quietly instead of leaving
                    // it around for a state that no longer reflects it.
                    tokenStorage.clearToken()
                }
                _authState.value = authStateForServerRejection(isExplicitAttempt, error)
                Result.Error(error)
            }
        } catch (e: CancellationException) {
            throw e
        } catch (e: Exception) {
            // Server unreachable — fall back to local-only auth
            log.w(e) { "Server verification failed, using local-only auth" }
            fallbackToLocalAuth(idToken, email, displayName, photoUrl)
        }
    }

    private suspend fun fallbackToLocalAuth(
        idToken: String,
        email: String?,
        displayName: String?,
        photoUrl: String?,
    ): Result<User, AuthError> {
        val user = User(
            id = idToken.take(32),
            email = email,
            displayName = displayName,
            photoUrl = photoUrl,
        )
        val storedToken = StoredAuthToken(
            accessToken = idToken,
            refreshToken = null,
            expiresAtMs = Clock.System.now().toEpochMilliseconds() + LOCAL_TOKEN_EXPIRY_MS,
            userId = user.id,
            email = user.email,
            displayName = user.displayName,
            photoUrl = user.photoUrl,
        )
        tokenStorage.saveToken(storedToken)
        scheduleTokenExpiry(storedToken.expiresAtMs)
        _authState.value = AuthState.Authenticated(user)
        return Result.Success(user)
    }

    override fun clearError() {
        if (_authState.value is AuthState.Failed) {
            _authState.value = AuthState.Unauthenticated
        }
    }

    private fun scheduleTokenExpiry(expiresAtMs: Long) {
        expiryJob?.cancel()
        val delayMs = expiresAtMs - Clock.System.now().toEpochMilliseconds()
        if (delayMs <= 0) {
            log.i { "Token already expired, attempting silent refresh" }
            expiryJob = scope.launch { attemptSilentRefresh() }
            return
        }
        log.d { "Scheduling token expiry in ${delayMs / 1000}s" }
        expiryJob = scope.launch {
            delay(delayMs)
            log.i { "Session token expired, attempting silent refresh" }
            attemptSilentRefresh()
        }
    }

    /**
     * One opportunistic, no-UI attempt to renew the session via [GoogleSignInBridge.signInSilently]
     * -- succeeds only if the platform can silently confirm the same Google account is still
     * authorized (currently just Android's Credential Manager). On success, re-verifies with the
     * server exactly like an explicit sign-in ([verifyWithServer]), which re-arms the next
     * [scheduleTokenExpiry] itself. On failure, falls back to the original behavior: clear the
     * stored token and drop to [AuthState.Unauthenticated].
     */
    private suspend fun attemptSilentRefresh() {
        when (val signIn = googleSignInBridge.signInSilently()) {
            is Result.Success -> {
                val googleToken = signIn.data
                log.i { "Silent session refresh succeeded for ${googleToken.email}" }
                verifyWithServer(
                    googleToken.idToken,
                    googleToken.email,
                    googleToken.displayName,
                    googleToken.photoUrl,
                    isExplicitAttempt = false,
                )
            }

            is Result.Error -> {
                log.i { "Silent session refresh not available: ${signIn.error.message}" }
                tokenStorage.clearToken()
                _authState.value = AuthState.Unauthenticated
            }
        }
    }

    private fun StoredAuthToken.toAuthState(): AuthState =
        AuthState.Authenticated(
            User(
                id = userId,
                email = email,
                displayName = displayName,
                photoUrl = photoUrl,
            ),
        )

    private companion object {
        // 1 hour token expiry for local-only auth fallback
        const val LOCAL_TOKEN_EXPIRY_MS = 60 * 60 * 1000L
    }
}
