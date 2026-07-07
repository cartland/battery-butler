package com.chriscartland.batterybutler.data.repository.auth

import co.touchlab.kermit.Logger
import com.chriscartland.batterybutler.datalocal.auth.LabsSessionStorage
import com.chriscartland.batterybutler.datanetwork.LabsAuthGateway
import com.chriscartland.batterybutler.datanetwork.apiKeyForMode
import com.chriscartland.batterybutler.datanetwork.auth.GoogleSignInBridge
import com.chriscartland.batterybutler.domain.model.AuthError
import com.chriscartland.batterybutler.domain.model.AuthState
import com.chriscartland.batterybutler.domain.model.LabsFirebaseApiKey
import com.chriscartland.batterybutler.domain.model.LabsProdGoogleOAuthClient
import com.chriscartland.batterybutler.domain.model.LabsStagingGoogleOAuthClient
import com.chriscartland.batterybutler.domain.model.NetworkMode
import com.chriscartland.batterybutler.domain.model.NetworkModeKeyedState
import com.chriscartland.batterybutler.domain.model.Result
import com.chriscartland.batterybutler.domain.model.User
import com.chriscartland.batterybutler.domain.repository.LabsAuthRepository
import com.chriscartland.batterybutler.domain.repository.NetworkModeRepository
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.distinctUntilChanged
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.flow.flatMapLatest
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.launch
import me.tatarka.inject.annotations.Inject

/**
 * Default [LabsAuthRepository]. Drives the Labs sign-in chain:
 *
 *   Google Sign-In (Labs OAuth client) --> Google ID token
 *     --> [LabsAuthGateway.signInToLabsWithGoogle] (signInWithIdp) --> Labs session
 *
 * The OAuth client is chosen by the currently-selected Labs network mode (staging vs prod). The
 * Google token's `aud` must be a Labs client the Labs Firebase project trusts, which is why this
 * uses [GoogleSignInBridge.signInWithClient] with the per-env client rather than the own-backend
 * [GoogleSignInBridge.signIn]. The Labs session lives in the singleton gateway (shared with the
 * sync calls); this repo just tracks UI state.
 *
 * [labsAuthState] is keyed by network mode via [NetworkModeKeyedState] (same key,
 * [apiKeyForMode], that [LabsAuthGateway] uses to partition its token sessions) rather than a
 * plain `MutableStateFlow` -- a bare field would show a *previous* environment's "signed in"
 * status right after switching Labs modes, since nothing would tell it the environment changed.
 * See `bb-labs-mode-auth-state` in TODO.md for the bug this replaced.
 *
 * Each key's [AuthState] starts as [AuthState.Unknown] and is resolved once from
 * [labsSessionStorage] -- a lightweight, per-environment "believed signed in" flag persisted
 * across process restarts (profile info only, no tokens, no expiry). This exists so that after
 * the OS kills and relaunches the process (auth state is otherwise in-memory only and would reset
 * to [AuthState.Unauthenticated] every launch), the UI doesn't show a "Sign in to Labs" prompt at
 * the same moment the already-synced local data is visible. It deliberately does *not* re-validate
 * a real session -- if the underlying Labs/Google session actually expired, that surfaces later via
 * the normal sync-failure path, not here. See `bb-labs-signout-clear` in TODO.md.
 */
@OptIn(ExperimentalCoroutinesApi::class)
@Inject
class DefaultLabsAuthRepository(
    private val googleSignInBridge: GoogleSignInBridge,
    private val labsAuthGateway: LabsAuthGateway,
    private val networkModeRepository: NetworkModeRepository,
    private val labsFirebaseApiKey: LabsFirebaseApiKey,
    private val labsStagingOAuthClient: LabsStagingGoogleOAuthClient,
    private val labsProdOAuthClient: LabsProdGoogleOAuthClient,
    private val labsSessionStorage: LabsSessionStorage,
    private val scope: CoroutineScope,
) : LabsAuthRepository {
    private val log = Logger.withTag("DefaultLabsAuthRepository")

    private val authStateByMode = NetworkModeKeyedState<AuthState>(
        networkMode = networkModeRepository.networkMode,
        keyFor = { apiKeyForMode(it, labsFirebaseApiKey) },
        default = AuthState.Unknown,
    )
    override val labsAuthState: Flow<AuthState> = authStateByMode.current

    init {
        scope.launch {
            networkModeRepository.networkMode
                .map { apiKeyForMode(it, labsFirebaseApiKey) }
                .distinctUntilChanged()
                .flatMapLatest { key -> labsSessionStorage.observeUser(key).map { key to it } }
                .distinctUntilChanged()
                .collect { (key, user) ->
                    val resolved = user?.let { AuthState.Authenticated(it) } ?: AuthState.Unauthenticated
                    authStateByMode.compareAndSet(key, expected = AuthState.Unknown, newValue = resolved)
                }
        }
    }

    override suspend fun signInToLabs(): Result<User, AuthError> {
        val client = when (networkModeRepository.networkMode.first()) {
            is NetworkMode.LabsStaging -> {
                labsStagingOAuthClient.clientId to labsStagingOAuthClient.clientSecret
            }

            is NetworkMode.LabsProd -> {
                labsProdOAuthClient.clientId to labsProdOAuthClient.clientSecret
            }

            else -> {
                return fail(
                    AuthError.Configuration.NotConfigured(
                        message = "Not a Labs network mode",
                        cause = "Select Labs (staging) or Labs (prod) before signing in to Labs",
                    ),
                )
            }
        }
        val (clientId, clientSecret) = client
        if (clientId.isBlank()) {
            return fail(
                AuthError.Configuration.NotConfigured(
                    message = "Labs sign-in not configured",
                    cause = "No Labs OAuth client ID for this environment (owner setup pending)",
                ),
            )
        }

        authStateByMode.setCurrent(AuthState.Authenticating)
        return when (val signIn = googleSignInBridge.signInWithClient(clientId, clientSecret.ifBlank { null })) {
            is Result.Success -> {
                exchangeForLabsSession(signIn.data)
            }

            is Result.Error -> {
                log.w { "Labs Google Sign-In failed: ${signIn.error.message}" }
                fail(signIn.error)
            }
        }
    }

    private suspend fun exchangeForLabsSession(
        google: com.chriscartland.batterybutler.datanetwork.auth.GoogleIdToken,
    ): Result<User, AuthError> =
        when (val exchange = labsAuthGateway.signInToLabsWithGoogle(google.idToken)) {
            is Result.Success -> {
                val user = User(
                    id = google.email ?: google.idToken.take(USER_ID_FALLBACK_LEN),
                    email = google.email,
                    displayName = google.displayName,
                    photoUrl = google.photoUrl,
                )
                log.i { "Labs sign-in successful for ${google.email}" }
                authStateByMode.setCurrent(AuthState.Authenticated(user))
                labsSessionStorage.saveUser(apiKeyForMode(networkModeRepository.networkMode.first(), labsFirebaseApiKey), user)
                Result.Success(user)
            }

            is Result.Error -> {
                log.w { "Labs token exchange failed: ${exchange.error.message}" }
                fail(exchange.error)
            }
        }

    override suspend fun signOutLabs() {
        labsAuthGateway.signOutLabs()
        authStateByMode.setCurrent(AuthState.Unauthenticated)
        labsSessionStorage.clearUser(apiKeyForMode(networkModeRepository.networkMode.first(), labsFirebaseApiKey))
    }

    override suspend fun clearError() {
        authStateByMode.updateCurrent { current ->
            if (current is AuthState.Failed) AuthState.Unauthenticated else current
        }
    }

    override suspend fun getLabsIdToken(): String? = labsAuthGateway.getLabsIdToken()

    private suspend fun fail(error: AuthError): Result<User, AuthError> {
        authStateByMode.setCurrent(AuthState.Failed(error))
        return Result.Error(error)
    }

    private companion object {
        const val USER_ID_FALLBACK_LEN = 32
    }
}
