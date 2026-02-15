package com.chriscartland.batterybutler.data.repository.auth

import co.touchlab.kermit.Logger
import com.chriscartland.batterybutler.data.di.Singleton
import com.chriscartland.batterybutler.datalocal.auth.AuthTokenStorage
import com.chriscartland.batterybutler.datalocal.auth.StoredAuthToken
import com.chriscartland.batterybutler.datanetwork.auth.GoogleSignInBridge
import com.chriscartland.batterybutler.domain.model.AuthError
import com.chriscartland.batterybutler.domain.model.AuthState
import com.chriscartland.batterybutler.domain.model.Result
import com.chriscartland.batterybutler.domain.model.User
import com.chriscartland.batterybutler.domain.provider.DispatcherProvider
import com.chriscartland.batterybutler.domain.repository.AuthRepository
import com.chriscartland.batterybutler.proto.AuthServiceClient
import com.chriscartland.batterybutler.proto.VerifyTokenRequest
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Job
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
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
 */
@OptIn(ExperimentalTime::class)
@Singleton
@Inject
class DefaultAuthRepository(
    private val googleSignInBridge: GoogleSignInBridge,
    private val authServiceClient: AuthServiceClient,
    private val tokenStorage: AuthTokenStorage,
    private val scope: CoroutineScope,
    private val dispatcherProvider: DispatcherProvider,
) : AuthRepository {
    private val log = Logger.withTag("DefaultAuthRepository")

    private val _authState = MutableStateFlow<AuthState>(AuthState.Unknown)
    override val authState: StateFlow<AuthState> = _authState.asStateFlow()
    private var expiryJob: Job? = null

    override val currentUser: Flow<User?> = authState.map { state ->
        when (state) {
            is AuthState.Authenticated -> state.user
            else -> null
        }
    }

    init {
        // Check for stored token on initialization
        scope.launch {
            tokenStorage.storedToken.collect { storedToken ->
                if (_authState.value is AuthState.Unknown) {
                    val newState = storedToken?.toAuthState() ?: AuthState.Unauthenticated
                    _authState.value = newState
                    if (newState is AuthState.Authenticated && storedToken != null) {
                        scheduleTokenExpiry(storedToken.expiresAtMs)
                    }
                }
            }
        }
    }

    override fun isSignInAvailable(): Boolean = googleSignInBridge.isConfigured()

    override suspend fun signInWithGoogle(): Result<User, AuthError> {
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

                verifyWithServer(googleToken.idToken, googleToken.email, googleToken.displayName, googleToken.photoUrl)
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
     */
    private suspend fun verifyWithServer(
        idToken: String,
        email: String?,
        displayName: String?,
        photoUrl: String?,
    ): Result<User, AuthError> =
        try {
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
                _authState.value = AuthState.Failed(error)
                Result.Error(error)
            }
        } catch (e: CancellationException) {
            throw e
        } catch (e: Exception) {
            // Server unreachable — fall back to local-only auth
            log.w(e) { "Server verification failed, using local-only auth" }
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
            Result.Success(user)
        }

    override fun clearError() {
        if (_authState.value is AuthState.Failed) {
            _authState.value = AuthState.Unauthenticated
        }
    }

    override suspend fun setExternalToken(token: String) {
        log.w { "Setting external token manually (Debug/Test only)" }
        // Create a fake user from the token structure if possible, or just a dummy user
        val user = User(
            id = "external-user",
            email = "test@example.com",
            displayName = "Test User",
            photoUrl = null,
        )
        val storedToken = StoredAuthToken(
            accessToken = token,
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
    }

    private fun scheduleTokenExpiry(expiresAtMs: Long) {
        expiryJob?.cancel()
        val delayMs = expiresAtMs - Clock.System.now().toEpochMilliseconds()
        if (delayMs <= 0) {
            log.i { "Token already expired, signing out" }
            _authState.value = AuthState.Unauthenticated
            return
        }
        log.d { "Scheduling token expiry in ${delayMs / 1000}s" }
        // Use Default dispatcher to avoid blocking Main thread idle checks in tests
        expiryJob = scope.launch(dispatcherProvider.default) {
            delay(delayMs)
            log.i { "Session token expired" }
            tokenStorage.clearToken()
            _authState.value = AuthState.Unauthenticated
        }
    }

    private fun StoredAuthToken.toAuthState(): AuthState {
        val now = Clock.System.now().toEpochMilliseconds()
        return if (expiresAtMs > now) {
            AuthState.Authenticated(
                User(
                    id = userId,
                    email = email,
                    displayName = displayName,
                    photoUrl = photoUrl,
                ),
            )
        } else {
            // Token expired
            AuthState.Unauthenticated
        }
    }

    private companion object {
        // 1 hour token expiry for local-only auth fallback
        const val LOCAL_TOKEN_EXPIRY_MS = 60 * 60 * 1000L
    }
}
