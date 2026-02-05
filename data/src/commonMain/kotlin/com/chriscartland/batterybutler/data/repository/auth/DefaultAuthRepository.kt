package com.chriscartland.batterybutler.data.repository.auth

import co.touchlab.kermit.Logger
import com.chriscartland.batterybutler.datalocal.auth.AuthTokenStorage
import com.chriscartland.batterybutler.datalocal.auth.StoredAuthToken
import com.chriscartland.batterybutler.datanetwork.auth.GoogleSignInBridge
import com.chriscartland.batterybutler.domain.model.AuthError
import com.chriscartland.batterybutler.domain.model.AuthState
import com.chriscartland.batterybutler.domain.model.Result
import com.chriscartland.batterybutler.domain.model.User
import com.chriscartland.batterybutler.domain.repository.AuthRepository
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.launch
import me.tatarka.inject.annotations.Inject
import kotlin.time.Clock
import kotlin.time.ExperimentalTime

/**
 * Default implementation of [AuthRepository].
 *
 * Handles:
 * - Google Sign-In flow via [GoogleSignInBridge]
 * - Token persistence via [AuthTokenStorage]
 * - Session state management
 *
 * Note: Server token verification is not yet implemented.
 * Currently stores the Google ID token directly (for local-only auth).
 */
@OptIn(ExperimentalTime::class)
@Inject
class DefaultAuthRepository(
    private val googleSignInBridge: GoogleSignInBridge,
    private val tokenStorage: AuthTokenStorage,
    private val scope: CoroutineScope,
) : AuthRepository {
    private val log = Logger.withTag("DefaultAuthRepository")

    private val _authState = MutableStateFlow<AuthState>(AuthState.Unknown)
    override val authState: StateFlow<AuthState> = _authState.asStateFlow()

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
                    _authState.value = storedToken?.toAuthState() ?: AuthState.Unauthenticated
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

                // TODO: Verify token with server and get session token
                // For now, use Google ID token directly (local-only auth)

                val user = User(
                    id = googleToken.idToken.take(32), // Use part of token as temp ID
                    email = googleToken.email,
                    displayName = googleToken.displayName,
                    photoUrl = googleToken.photoUrl,
                )

                // Store the token
                val storedToken = StoredAuthToken(
                    accessToken = googleToken.idToken,
                    refreshToken = null, // No refresh token without server
                    expiresAtMs = Clock.System.now().toEpochMilliseconds() + TOKEN_EXPIRY_MS,
                    userId = user.id,
                    email = user.email,
                    displayName = user.displayName,
                    photoUrl = user.photoUrl,
                )
                tokenStorage.saveToken(storedToken)

                _authState.value = AuthState.Authenticated(user)
                Result.Success(user)
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
        googleSignInBridge.signOut()
        tokenStorage.clearToken()
        _authState.value = AuthState.Unauthenticated
    }

    override suspend fun refreshToken(): Result<Unit, AuthError> {
        // TODO: Implement token refresh with server
        // For now, if token is expired, user needs to sign in again
        val currentToken = tokenStorage.storedToken
        log.w { "Token refresh not implemented" }
        return Result.Error(
            AuthError.Token.Expired(
                message = "Session expired",
                cause = "Token refresh not yet implemented",
            ),
        )
    }

    override fun clearError() {
        if (_authState.value is AuthState.Failed) {
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
        // 1 hour token expiry for local-only auth
        const val TOKEN_EXPIRY_MS = 60 * 60 * 1000L
    }
}
