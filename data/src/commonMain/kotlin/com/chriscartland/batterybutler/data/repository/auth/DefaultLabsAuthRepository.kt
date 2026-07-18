package com.chriscartland.batterybutler.data.repository.auth

import co.touchlab.kermit.Logger
import com.chriscartland.batterybutler.datalocal.auth.LabsSessionStorage
import com.chriscartland.batterybutler.datanetwork.LabsAuthGateway
import com.chriscartland.batterybutler.datanetwork.apiKeyForMode
import com.chriscartland.batterybutler.datanetwork.auth.GoogleSignInBridge
import com.chriscartland.batterybutler.domain.model.AuthError
import com.chriscartland.batterybutler.domain.model.AuthState
import com.chriscartland.batterybutler.domain.model.DataMode
import com.chriscartland.batterybutler.domain.model.DataModeKeyedState
import com.chriscartland.batterybutler.domain.model.LabsFirebaseApiKey
import com.chriscartland.batterybutler.domain.model.LabsProdGoogleOAuthClient
import com.chriscartland.batterybutler.domain.model.LabsStagingGoogleOAuthClient
import com.chriscartland.batterybutler.domain.model.Result
import com.chriscartland.batterybutler.domain.model.User
import com.chriscartland.batterybutler.domain.repository.DataModeRepository
import com.chriscartland.batterybutler.domain.repository.LabsAuthRepository
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
 * The OAuth client is chosen by the currently-selected Labs data mode (staging vs prod). The
 * Google token's `aud` must be a Labs client the Labs Firebase project trusts, which is why this
 * uses [GoogleSignInBridge.signInWithClient] with the per-env client rather than the own-backend
 * [GoogleSignInBridge.signIn]. The Labs session lives in the singleton gateway (shared with the
 * sync calls); this repo just tracks UI state.
 *
 * [labsAuthState] is keyed by data mode via [DataModeKeyedState] (same key,
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
 *
 * Right after resolving `Unknown` from persisted storage this way, [restoreLabsSession] also makes
 * one opportunistic attempt to re-establish the *real* [labsAuthGateway] session, via
 * [LabsAuthGateway.restoreSession] -- a plain, non-interactive network call against a persisted
 * Labs refresh token, never [GoogleSignInBridge]'s silent Credential Manager path. This is
 * best-effort: on a transient failure (no persisted token yet, network unreachable) it changes
 * nothing (no UI impact, no state rollback) and background sync simply keeps failing until the
 * user explicitly signs in again, exactly as it would without this attempt at all. Only an
 * *authoritative* rejection (the refresh token was actually revoked) clears the belief and flips
 * to [AuthState.Unauthenticated]. See `bb-labs-persist-signin-belief` and
 * `bb-labs-refresh-token-persistence` in TODO.md.
 *
 * An earlier version of this attempt used [GoogleSignInBridge.signInSilentlyWithClient], which is
 * *not* actually guaranteed headless on Android -- it's the same Credential Manager `getCredential`
 * call as the interactive picker, differing only by `filterByAuthorizedAccounts`, and could still
 * surface a chooser/bottom-sheet on every process (re)start. [LabsAuthGateway.restoreSession] has
 * no such risk (it's a plain HTTP call), so it needs no cooldown and is safe to run on every start.
 * See `bb-silent-reauth-cooldown` in TODO.md for the bug this replaced.
 */
@OptIn(ExperimentalCoroutinesApi::class)
@Inject
class DefaultLabsAuthRepository(
    private val googleSignInBridge: GoogleSignInBridge,
    private val labsAuthGateway: LabsAuthGateway,
    private val dataModeRepository: DataModeRepository,
    private val labsFirebaseApiKey: LabsFirebaseApiKey,
    private val labsStagingOAuthClient: LabsStagingGoogleOAuthClient,
    private val labsProdOAuthClient: LabsProdGoogleOAuthClient,
    private val labsSessionStorage: LabsSessionStorage,
    private val scope: CoroutineScope,
) : LabsAuthRepository {
    private val log = Logger.withTag("DefaultLabsAuthRepository")

    private val authStateByMode = DataModeKeyedState<AuthState>(
        dataMode = dataModeRepository.dataMode,
        keyFor = { apiKeyForMode(it, labsFirebaseApiKey) },
        default = AuthState.Unknown,
    )
    override val labsAuthState: Flow<AuthState> = authStateByMode.current

    init {
        scope.launch {
            dataModeRepository.dataMode
                .map { apiKeyForMode(it, labsFirebaseApiKey) }
                .distinctUntilChanged()
                .flatMapLatest { key -> labsSessionStorage.observeUser(key).map { key to it } }
                .distinctUntilChanged()
                .collect { (key, user) ->
                    val resolved = user?.let { AuthState.Authenticated(it) } ?: AuthState.Unauthenticated
                    val resolvedFromUnknown = authStateByMode.compareAndSet(key, expected = AuthState.Unknown, newValue = resolved)
                    if (resolvedFromUnknown && user != null) {
                        restoreLabsSession(key)
                    }
                }
        }
    }

    /** Staging/prod OAuth client for whichever Labs mode is selected right now, or null if not a Labs mode. */
    private suspend fun labsOAuthClient(): Pair<String, String>? =
        when (dataModeRepository.dataMode.first()) {
            is DataMode.LabsStaging -> labsStagingOAuthClient.clientId to labsStagingOAuthClient.clientSecret
            is DataMode.LabsProd -> labsProdOAuthClient.clientId to labsProdOAuthClient.clientSecret
            else -> null
        }

    /**
     * One opportunistic attempt to re-establish the real gateway session right after resolving a
     * persisted "believed signed in" belief, via [LabsAuthGateway.restoreSession] -- a plain
     * network call against a persisted refresh token, no OS-UI risk, so (unlike the
     * Credential-Manager-based attempt this replaced) it needs no cooldown and is safe to run on
     * every process (re)start. On success or a transient failure, never touches [authStateByMode]
     * or the believed-signed-in user in [labsSessionStorage] -- success needs no state change
     * (already Authenticated from the belief), and a transient failure (no persisted token yet,
     * network unreachable) is left silent by design so the believed-signed-in UI doesn't flip back
     * to a sign-in prompt just because this best-effort attempt didn't pan out. Only an
     * *authoritative* rejection ([AuthError.Token.Invalid] -- the refresh token was actually
     * revoked) clears the belief and flips to [AuthState.Unauthenticated], silently (no error
     * dialog -- this is a background-triggered transition, not a user-waited-on one).
     */
    private suspend fun restoreLabsSession(key: String) {
        if (labsOAuthClient() == null) return

        when (val result = labsAuthGateway.restoreSession()) {
            is Result.Success -> {
                log.i { "Labs session restored from persisted refresh token" }
            }

            is Result.Error -> {
                if (result.error is AuthError.Token.Invalid) {
                    log.i { "Labs refresh token rejected; clearing believed-signed-in state" }
                    authStateByMode.setCurrent(AuthState.Unauthenticated)
                    labsSessionStorage.clearUser(key)
                } else {
                    log.i { "Labs session restore not available yet: ${result.error.message}" }
                }
            }
        }
    }

    override suspend fun signInToLabs(): Result<User, AuthError> {
        val client = labsOAuthClient()
            ?: return fail(
                AuthError.Configuration.NotConfigured(
                    message = "Not a Labs data mode",
                    cause = "Select Labs (staging) or Labs (prod) before signing in to Labs",
                ),
            )
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
                labsSessionStorage.saveUser(apiKeyForMode(dataModeRepository.dataMode.first(), labsFirebaseApiKey), user)
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
        labsSessionStorage.clearUser(apiKeyForMode(dataModeRepository.dataMode.first(), labsFirebaseApiKey))
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
