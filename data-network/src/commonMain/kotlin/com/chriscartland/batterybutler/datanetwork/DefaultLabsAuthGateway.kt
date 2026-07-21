package com.chriscartland.batterybutler.datanetwork

import co.touchlab.kermit.Logger
import com.chriscartland.batterybutler.datanetwork.rest.FirebaseIdTokenProvider
import com.chriscartland.batterybutler.datanetwork.rest.createSyncHttpClient
import com.chriscartland.batterybutler.domain.model.AuthError
import com.chriscartland.batterybutler.domain.model.DataMode
import com.chriscartland.batterybutler.domain.model.LabsFirebaseApiKey
import com.chriscartland.batterybutler.domain.model.Result
import com.chriscartland.batterybutler.domain.model.SyncAuthReason
import com.chriscartland.batterybutler.domain.repository.DataModeRepository
import com.chriscartland.batterybutler.domain.repository.LabsRefreshTokenPersistence
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.MutableSharedFlow
import kotlinx.coroutines.flow.distinctUntilChanged
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.launch
import kotlinx.coroutines.sync.Mutex
import kotlinx.coroutines.sync.withLock
import me.tatarka.inject.annotations.Inject

/**
 * The Labs Firebase **Web API key** for [mode]'s project: the prod key in a prod Labs mode, else the
 * staging key. `signInWithIdp` mints a Firebase ID token from whichever project owns the key, so the
 * key must match the env's backend (a staging token is rejected by the prod backend and vice versa).
 * Blank when that env is unconfigured.
 */
fun apiKeyForMode(
    mode: DataMode,
    keys: LabsFirebaseApiKey,
): String =
    when (mode) {
        is DataMode.LabsProd -> keys.prod
        else -> keys.staging
    }

/**
 * Default [LabsAuthGateway] — owns the per-env [FirebaseIdTokenProvider] session(s).
 *
 * Provided as a DI singleton (see AppComponent) so a session is shared between the sign-in trigger
 * and [DelegatingRemoteDataSource]'s token reads. The Firebase Web API key is selected by the
 * **current [DataMode]** (staging vs prod are separate Firebase projects — see [apiKeyForMode]),
 * and one provider is kept per env key so a mode switch uses that env's session. The provider map
 * is guarded by a [Mutex] — it is reached concurrently from the sync loop, sign-in, and the
 * proactive-refresh plumbing, and an unguarded `getOrPut` can construct two providers for one env
 * and silently drop one (with it, whichever session that one held). The HTTP client + providers
 * are built lazily, so nothing is created until a Labs mode is actually used. Keys are blank until
 * owner setup; the provider then reports `NotConfigured` and yields no token, so the app degrades
 * (sends no Bearer header → 401) rather than crashing.
 *
 * [refreshTokenPersistence] durably persists each provider's refresh token so [restoreSession]
 * can rebuild a session after this in-memory instance is recreated on process restart, without
 * ever falling back to Google/Credential Manager for that (see
 * `bb-labs-refresh-token-persistence` in TODO.md). Persistence is write-through on every *applied*
 * rotation (sign-in, restore, and — new — every silent/forced/proactive refresh, since Firebase
 * may rotate the refresh token on any of them; a rotation that wasn't persisted meant the next
 * cold-start restore presented a stale token and failed). Writes happen only when the value
 * actually changed, so refreshes don't ripple no-op edits into the shared DataStore.
 *
 * Session loss is **reactive and event-driven**: any authoritative rejection — a refresh reporting
 * the token invalid (silent, forced, restore, or proactive) or a terminal 401 delivered via
 * [reportSessionRejected] — funnels through one path that clears the in-memory session + the
 * persisted refresh token and emits on [sessionInvalidations]. The auth repository owns the state
 * flip; local device data is never touched from here.
 */
@Inject
class DefaultLabsAuthGateway(
    private val dataModeRepository: DataModeRepository,
    private val labsFirebaseApiKey: LabsFirebaseApiKey,
    private val refreshTokenPersistence: LabsRefreshTokenPersistence,
    private val scope: CoroutineScope,
) : LabsAuthGateway {
    private val log = Logger.withTag("DefaultLabsAuthGateway")
    private val httpClient by lazy { createSyncHttpClient() }

    // One session per env Firebase key. Keyed by apiKey so staging and prod each keep their own
    // in-memory session across mode switches; created on first use of that env. Guarded by
    // [providersMutex]; internal so tests can assert single-instance behavior under concurrency.
    private val providersMutex = Mutex()
    internal val providersByApiKey = mutableMapOf<String, FirebaseIdTokenProvider>()

    /** Total provider constructions — observable proof that concurrent callers share one instance per env. */
    internal var providerConstructionCount = 0

    private val _sessionInvalidations = MutableSharedFlow<LabsSessionInvalidation>(extraBufferCapacity = INVALIDATION_BUFFER)
    override val sessionInvalidations: Flow<LabsSessionInvalidation> = _sessionInvalidations

    init {
        // Proactive refresh follows the *selected* environment: enable it on the current env's
        // provider, disable it on the others, on every mode switch. Providers created later for
        // the current mode enable themselves at construction time (providerForCurrentMode).
        scope.launch {
            dataModeRepository.dataMode
                .map { apiKeyForMode(it, labsFirebaseApiKey) }
                .distinctUntilChanged()
                .collect { activeKey ->
                    val snapshot = providersMutex.withLock { providersByApiKey.toMap() }
                    snapshot.forEach { (key, provider) -> provider.setProactiveRefresh(key == activeKey) }
                }
        }
    }

    private suspend fun providerForCurrentMode(): Pair<String, FirebaseIdTokenProvider> {
        val apiKey = apiKeyForMode(dataModeRepository.dataMode.first(), labsFirebaseApiKey)
        val (provider, created) = providersMutex.withLock {
            val existing = providersByApiKey[apiKey]
            if (existing != null) {
                existing to false
            } else {
                val fresh = FirebaseIdTokenProvider(
                    httpClient = httpClient,
                    apiKey = apiKey,
                    scope = scope,
                    onRefreshTokenRotated = { token -> persistRotatedToken(apiKey, token) },
                )
                providersByApiKey[apiKey] = fresh
                providerConstructionCount++
                fresh to true
            }
        }
        // Created for the mode that is current right now, so proactive refresh starts enabled;
        // the init collector flips it off if the mode moves elsewhere.
        if (created) provider.setProactiveRefresh(true)
        return apiKey to provider
    }

    /**
     * Persists [refreshToken] for [environmentKey] iff it differs from what's already persisted —
     * every applied refresh reports its token here, and Firebase only occasionally rotates it, so
     * the compare keeps refreshes from rippling no-op writes into the shared DataStore. Internal
     * (not private) so the write-only-on-change contract is directly unit-testable without HTTP.
     */
    internal suspend fun persistRotatedToken(
        environmentKey: String,
        refreshToken: String,
    ) {
        if (refreshTokenPersistence.get(environmentKey) != refreshToken) {
            log.i { "Persisting rotated Labs refresh token" }
            refreshTokenPersistence.save(environmentKey, refreshToken)
        }
    }

    override suspend fun signInToLabsWithGoogle(googleIdToken: String): Result<Unit, AuthError> {
        val (_, provider) = providerForCurrentMode()
        // A successful sign-in persists its refresh token via the provider's rotation callback.
        return provider.signInWithGoogle(googleIdToken)
    }

    override suspend fun getLabsToken(forceRefresh: Boolean): LabsTokenResult {
        val (apiKey, provider) = providerForCurrentMode()
        return when (val outcome = provider.getToken(forceRefresh)) {
            is FirebaseIdTokenProvider.TokenOutcome.Token -> {
                LabsTokenResult.Token(outcome.idToken, servedFromCache = outcome.servedFromCache)
            }

            FirebaseIdTokenProvider.TokenOutcome.NoSession -> {
                restoreOnDemand(apiKey, provider)
            }

            FirebaseIdTokenProvider.TokenOutcome.Invalid -> {
                invalidateSession(apiKey, provider, SyncAuthReason.TOKEN_INVALID)
                LabsTokenResult.SessionInvalidated
            }

            FirebaseIdTokenProvider.TokenOutcome.TransientFailure -> {
                LabsTokenResult.TransientFailure
            }
        }
    }

    /**
     * No in-memory session — try the persisted refresh token before reporting
     * [LabsTokenResult.NoSession], so a cold start (or an earlier transient restore failure)
     * self-heals on the next token read instead of churning "sign in required".
     */
    private suspend fun restoreOnDemand(
        apiKey: String,
        provider: FirebaseIdTokenProvider,
    ): LabsTokenResult {
        val persisted = refreshTokenPersistence.get(apiKey) ?: return LabsTokenResult.NoSession
        return when (val restore = provider.restoreSession(persisted)) {
            is Result.Success -> {
                when (val outcome = provider.getToken()) {
                    is FirebaseIdTokenProvider.TokenOutcome.Token -> {
                        LabsTokenResult.Token(outcome.idToken, servedFromCache = false)
                    }

                    // The freshly-restored session vanished between calls (concurrent sign-out).
                    else -> {
                        LabsTokenResult.NoSession
                    }
                }
            }

            is Result.Error -> {
                if (restore.error is AuthError.Token.Invalid) {
                    invalidateSession(apiKey, provider, SyncAuthReason.TOKEN_INVALID)
                    LabsTokenResult.SessionInvalidated
                } else {
                    LabsTokenResult.TransientFailure
                }
            }
        }
    }

    override suspend fun getLabsIdToken(): String? = (getLabsToken() as? LabsTokenResult.Token)?.idToken

    override suspend fun reportSessionRejected(reason: SyncAuthReason) {
        val (apiKey, provider) = providerForCurrentMode()
        log.i { "Labs session rejected by the backend ($reason); tearing the session down" }
        invalidateSession(apiKey, provider, reason)
    }

    /**
     * The single session-loss path: clear the in-memory session, clear the persisted refresh
     * token, then emit so the auth repository can flip that env's state. Local device data is
     * deliberately untouched — only the explicit sign-out use case clears it.
     */
    private suspend fun invalidateSession(
        apiKey: String,
        provider: FirebaseIdTokenProvider,
        reason: SyncAuthReason,
    ) {
        provider.signOut()
        refreshTokenPersistence.clear(apiKey)
        _sessionInvalidations.emit(LabsSessionInvalidation(environmentKey = apiKey, reason = reason))
    }

    override suspend fun signOutLabs() {
        val (apiKey, provider) = providerForCurrentMode()
        provider.signOut()
        refreshTokenPersistence.clear(apiKey)
    }

    override suspend fun restoreSession(): Result<Unit, AuthError> {
        val (apiKey, provider) = providerForCurrentMode()
        val refreshToken = refreshTokenPersistence.get(apiKey)
            ?: return Result.Error(AuthError.Token.Expired(message = "No persisted Labs session"))
        val result = provider.restoreSession(refreshToken)
        // A successful restore persists any rotated token via the provider's rotation callback.
        if (result is Result.Error && result.error is AuthError.Token.Invalid) {
            invalidateSession(apiKey, provider, SyncAuthReason.TOKEN_INVALID)
        }
        return result
    }

    private companion object {
        /**
         * Invalidation events are rare (one per authoritative rejection) and the collector (the
         * auth repository) is registered at construction, so a small buffer only papers over a
         * startup race; DROP is unacceptable for a sign-out signal, hence suspend-on-overflow.
         */
        const val INVALIDATION_BUFFER = 8
    }
}
