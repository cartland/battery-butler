package com.chriscartland.batterybutler.data.repository.auth

import co.touchlab.kermit.Logger
import com.chriscartland.batterybutler.datalocal.auth.LabsSessionStorage
import com.chriscartland.batterybutler.datanetwork.LabsAuthGateway
import com.chriscartland.batterybutler.datanetwork.LabsSignInIdentity
import com.chriscartland.batterybutler.datanetwork.apiKeyForMode
import com.chriscartland.batterybutler.datanetwork.auth.GoogleIdToken
import com.chriscartland.batterybutler.datanetwork.auth.GoogleSignInBridge
import com.chriscartland.batterybutler.domain.model.AuthError
import com.chriscartland.batterybutler.domain.model.AuthState
import com.chriscartland.batterybutler.domain.model.DataMode
import com.chriscartland.batterybutler.domain.model.DataModeKeyedState
import com.chriscartland.batterybutler.domain.model.LabsFirebaseApiKey
import com.chriscartland.batterybutler.domain.model.LabsProdGoogleOAuthClient
import com.chriscartland.batterybutler.domain.model.LabsSessionRestoreResult
import com.chriscartland.batterybutler.domain.model.LabsStagingGoogleOAuthClient
import com.chriscartland.batterybutler.domain.model.Result
import com.chriscartland.batterybutler.domain.model.SignedOutCause
import com.chriscartland.batterybutler.domain.model.User
import com.chriscartland.batterybutler.domain.repository.DataModeRepository
import com.chriscartland.batterybutler.domain.repository.LabsAuthRepository
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Deferred
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.async
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.distinctUntilChanged
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.flow.flatMapLatest
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.launch
import kotlinx.coroutines.sync.Mutex
import kotlinx.coroutines.sync.withLock
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
 * the same moment the already-synced local data is visible. See `bb-labs-signout-clear` in
 * TODO.md.
 *
 * ## Cold-start session restore (the sync gate)
 *
 * The *real* gateway session is re-established from a persisted refresh token via
 * [LabsAuthGateway.restoreSession] -- a plain, non-interactive network call, never
 * [GoogleSignInBridge]'s silent Credential Manager path (which is not actually
 * headless-guaranteed on Android; see `bb-silent-reauth-cooldown` in TODO.md). The restore is
 * **single-flight per environment** and its resolution is exposed through
 * [awaitLabsSessionRestore]: the sync layer awaits it before firing its first Labs request, so a
 * believed-signed-in cold start no longer races the restore and churns "sign in required"
 * (`AuthRequired(NO_SESSION)`) while it is still in flight. Two triggers share the single flight:
 * the init collector fires it as soon as a believed-signed-in user is resolved (when in a Labs
 * mode), and [awaitLabsSessionRestore] fires it lazily on first demand otherwise (e.g. cold start
 * in Mock, then a switch to a Labs mode). A transient failure resolves as
 * [LabsSessionRestoreResult.TRANSIENT_FAILURE] -- never blocking sync, which then surfaces
 * *network* errors; the gateway's token path re-attempts the restore on demand, so a recovered
 * network heals without user action.
 *
 * ## Reactive session loss
 *
 * [LabsAuthGateway.sessionInvalidations] is the single signal that an environment's session was
 * authoritatively rejected (a refresh reported the token invalid, or a terminal 401 after the
 * wire layer's retry-once policy). By the time it is observed, the gateway has already cleared
 * the in-memory session + persisted refresh token; this repository completes the reaction by
 * flipping that environment's state to [AuthState.Unauthenticated] with
 * [SignedOutCause.SESSION_EXPIRED] and clearing the believed-signed-in user. Local device data
 * is deliberately NOT touched -- the user did not sign out, so the cached list stays visible
 * alongside the sign-in prompt; only the explicit sign-out use case clears local data.
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

    // Single-flight, per-environment session restore. Guarded by [restoreMutex]; a key resolves
    // exactly once (sticky) unless a later definitive transition (sign-in, sign-out,
    // invalidation) overwrites the resolution with its own.
    private val restoreMutex = Mutex()
    private val restoreResults = mutableMapOf<String, LabsSessionRestoreResult>()
    private val restoreInFlight = mutableMapOf<String, Deferred<LabsSessionRestoreResult>>()

    init {
        scope.launch {
            dataModeRepository.dataMode
                .map { apiKeyForMode(it, labsFirebaseApiKey) }
                .distinctUntilChanged()
                .flatMapLatest { key -> labsSessionStorage.observeUser(key).map { key to it } }
                .distinctUntilChanged()
                .collect { (key, user) ->
                    val resolved = user?.let { AuthState.Authenticated(it) } ?: AuthState.Unauthenticated()
                    val resolvedFromUnknown = authStateByMode.compareAndSet(key, expected = AuthState.Unknown, newValue = resolved)
                    // Kick the real session restore as soon as a believed-signed-in user is
                    // resolved -- but only in a Labs mode (elsewhere the restore is deferred to
                    // awaitLabsSessionRestore's lazy trigger, so a non-Labs cold start does no
                    // needless network work).
                    if (resolvedFromUnknown && user != null && labsOAuthClient() != null) {
                        restoreOnce(key)
                    }
                }
        }
        scope.launch {
            labsAuthGateway.sessionInvalidations.collect { invalidation ->
                log.i { "Labs session invalidated (${invalidation.reason}); flipping to session-expired sign-out" }
                authStateByMode.setFor(
                    invalidation.environmentKey,
                    AuthState.Unauthenticated(cause = SignedOutCause.SESSION_EXPIRED),
                )
                labsSessionStorage.clearUser(invalidation.environmentKey)
                markRestoreResolved(invalidation.environmentKey, LabsSessionRestoreResult.INVALID)
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

    override suspend fun awaitLabsSessionRestore(): LabsSessionRestoreResult {
        val key = apiKeyForMode(dataModeRepository.dataMode.first(), labsFirebaseApiKey)
        return restoreOnce(key)
    }

    /**
     * Returns [key]'s restore resolution, triggering the (single-flight) restore if it has not
     * started. Concurrent callers -- the init collector, the sync loop's gate, `resync()` -- all
     * await the same attempt; once resolved, every later call returns immediately.
     */
    private suspend fun restoreOnce(key: String): LabsSessionRestoreResult {
        val inFlight = restoreMutex.withLock {
            restoreResults[key]?.let { return it }
            restoreInFlight.getOrPut(key) {
                scope.async {
                    val result = doRestore(key)
                    restoreMutex.withLock {
                        // A definitive transition (sign-in / invalidation) may have resolved the
                        // key while the restore was in flight; its resolution wins.
                        restoreResults.getOrPut(key) { result }
                        restoreInFlight.remove(key)
                    }
                    result
                }
            }
        }
        return inFlight.await()
    }

    /** Records a definitive resolution for [key] (sign-in, sign-out, invalidation, restore outcome). */
    private suspend fun markRestoreResolved(
        key: String,
        result: LabsSessionRestoreResult,
    ) {
        restoreMutex.withLock { restoreResults[key] = result }
    }

    /**
     * One restore attempt for [key]: no believed user -> [LabsSessionRestoreResult.NO_SESSION]
     * (definitive -- sync proceeds and correctly reports sign-in required); otherwise
     * [LabsAuthGateway.restoreSession] against the persisted refresh token. On success or a
     * transient failure, never touches [authStateByMode] or the believed-signed-in user --
     * success needs no state change (already Authenticated from the belief), and a transient
     * failure (network unreachable) is left silent by design so the believed-signed-in UI doesn't
     * flip to a sign-in prompt just because this best-effort attempt didn't pan out. An
     * *authoritative* rejection surfaces via the gateway's [LabsAuthGateway.sessionInvalidations]
     * event, whose collector does the state flip + belief clear -- not here, so every
     * invalidation path shares one reaction.
     */
    private suspend fun doRestore(key: String): LabsSessionRestoreResult {
        val believedUser = labsSessionStorage.observeUser(key).first()
        if (believedUser == null) {
            return LabsSessionRestoreResult.NO_SESSION
        }
        return when (val result = labsAuthGateway.restoreSession()) {
            is Result.Success -> {
                log.i { "Labs session restored from persisted refresh token" }
                LabsSessionRestoreResult.RESTORED
            }

            is Result.Error -> {
                when (result.error) {
                    is AuthError.Token.Invalid -> {
                        log.i { "Labs refresh token rejected; session-loss reaction runs via the invalidation event" }
                        LabsSessionRestoreResult.INVALID
                    }

                    // "No persisted Labs session" / unconfigured env: nothing restorable. Definitive.
                    is AuthError.Token.Expired, is AuthError.Configuration.NotConfigured -> {
                        log.i { "Labs session not restorable: ${result.error.message}" }
                        LabsSessionRestoreResult.NO_SESSION
                    }

                    else -> {
                        log.i { "Labs session restore failed transiently: ${result.error.message}" }
                        LabsSessionRestoreResult.TRANSIENT_FAILURE
                    }
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

    private suspend fun exchangeForLabsSession(google: GoogleIdToken): Result<User, AuthError> =
        when (val exchange = labsAuthGateway.signInToLabsWithGoogle(google.idToken)) {
            is Result.Success -> {
                val user = labsUserFrom(identity = exchange.data, google = google)
                log.i { "Labs sign-in successful for ${google.email}" }
                authStateByMode.setCurrent(AuthState.Authenticated(user))
                val key = apiKeyForMode(dataModeRepository.dataMode.first(), labsFirebaseApiKey)
                labsSessionStorage.saveUser(key, user)
                // A live session now exists; the cold-start gate must not wait on (or re-run) a
                // restore for this environment.
                markRestoreResolved(key, LabsSessionRestoreResult.RESTORED)
                Result.Success(user)
            }

            is Result.Error -> {
                log.w { "Labs token exchange failed: ${exchange.error.message}" }
                fail(exchange.error)
            }
        }

    override suspend fun signOutLabs() {
        labsAuthGateway.signOutLabs()
        authStateByMode.setCurrent(AuthState.Unauthenticated())
        val key = apiKeyForMode(dataModeRepository.dataMode.first(), labsFirebaseApiKey)
        labsSessionStorage.clearUser(key)
        // Nothing left to restore for this environment; the gate resolves immediately.
        markRestoreResolved(key, LabsSessionRestoreResult.NO_SESSION)
    }

    override suspend fun clearError() {
        authStateByMode.updateCurrent { current ->
            if (current is AuthState.Failed) AuthState.Unauthenticated() else current
        }
    }

    override suspend fun getLabsIdToken(): String? = labsAuthGateway.getLabsIdToken()

    private suspend fun fail(error: AuthError): Result<User, AuthError> {
        authStateByMode.setCurrent(AuthState.Failed(error))
        return Result.Error(error)
    }
}

/**
 * Builds the signed-in [User] from a successful Labs token exchange.
 *
 * [User.id] is the **Firebase uid** ([LabsSignInIdentity.firebaseUid], `signInWithIdp`'s
 * `localId`) — the id the Labs backend actually authorizes and attributes writes with (e.g.
 * device-image `uploadedByUid`), not the Google profile email the previous derivation used. The
 * email/token-prefix fallbacks only fire if the wire response omitted the uid (lenient-consumer
 * defaults). Existing installs may still hold an email-shaped id in the persisted believed user;
 * that value is display-time only and is overwritten by the next successful sign-in — no
 * migration needed.
 */
internal fun labsUserFrom(
    identity: LabsSignInIdentity,
    google: GoogleIdToken,
): User =
    User(
        id = identity.firebaseUid ?: google.email ?: google.idToken.take(USER_ID_FALLBACK_LEN),
        email = identity.email ?: google.email,
        displayName = google.displayName,
        photoUrl = google.photoUrl,
    )

private const val USER_ID_FALLBACK_LEN = 32
