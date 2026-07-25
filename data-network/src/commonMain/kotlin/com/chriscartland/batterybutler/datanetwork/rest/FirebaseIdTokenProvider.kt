package com.chriscartland.batterybutler.datanetwork.rest

import co.touchlab.kermit.Logger
import com.chriscartland.batterybutler.datanetwork.LabsSignInIdentity
import com.chriscartland.batterybutler.domain.model.AuthError
import com.chriscartland.batterybutler.domain.model.Result
import io.ktor.client.HttpClient
import io.ktor.client.call.body
import io.ktor.client.request.parameter
import io.ktor.client.request.post
import io.ktor.client.request.setBody
import io.ktor.http.ContentType
import io.ktor.http.contentType
import io.ktor.http.isSuccess
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Deferred
import kotlinx.coroutines.Job
import kotlinx.coroutines.async
import kotlinx.coroutines.delay
import kotlinx.coroutines.launch
import kotlinx.coroutines.sync.Mutex
import kotlinx.coroutines.sync.withLock
import kotlin.coroutines.cancellation.CancellationException
import kotlin.time.Clock
import kotlin.time.ExperimentalTime

/**
 * Supplies a Labs **Firebase ID token** for the `Authorization: Bearer` header on the Labs
 * `/v1/battery-butler/sync` endpoint, via the Firebase Auth REST IdP flow (proven by the
 * Workstream D spike):
 *
 * ```
 * Google ID token --accounts:signInWithIdp--> Labs ID token + refresh token   (interactive, once)
 *   refresh token --securetoken /v1/token--> fresh Labs ID token              (non-interactive)
 * ```
 *
 * [signInWithGoogle] is the one interactive step (it needs a Google ID token whose audience the
 * Labs Firebase project trusts — the OAuth-client owner setup). Afterwards [getToken] refreshes
 * silently with the stored refresh token, and [restoreSession] can rebuild a session from a
 * *persisted* refresh token (e.g. after this in-memory instance is recreated on process restart)
 * without any Google/Credential Manager involvement — see `bb-labs-refresh-token-persistence` in
 * TODO.md for why that matters (Credential Manager's "silent" sign-in is not actually
 * headless-guaranteed on Android). The HTTP boundary is wrapped (project rule: never throw
 * except [CancellationException]); a failure returns a typed outcome/[Result.Error] so an
 * unconfigured or signed-out app sends no header — it degrades, never crashes.
 *
 * ## Concurrency
 *
 * Refresh is **single-flight and off-mutex**: the mutex guards only the in-memory state, never a
 * network call (an earlier version held it across the securetoken round-trip, serializing every
 * concurrent token read behind the network). Concurrent callers needing a refresh all await the
 * same [Deferred]; the result is applied exactly once, guarded by a session epoch so a refresh
 * that lands after [signOut]/a new [signInWithGoogle] can't resurrect or clobber the session.
 *
 * ## Proactive refresh
 *
 * With [setProactiveRefresh] enabled (the gateway enables it for the *selected* environment's
 * provider), a background task on [scope] sleeps until `expiresAt - EXPIRY_BUFFER_MS` and
 * refreshes then — so user-facing requests ~never pay refresh latency mid-request (measured at
 * ~3.4s per sync when they did). The task reschedules itself off every applied refresh and is
 * cancelled on sign-out / disable.
 *
 * Stateful + shared across sync calls, so construct once per environment (the gateway keys
 * instances by env API key).
 *
 * @param apiKey the Labs Firebase **Web API key** (Workstream E injects it; blank = unconfigured).
 * @param scope app-lifetime scope hosting the single-flight refresh + the proactive-refresh task.
 * @param now epoch-millis source, injectable for tests.
 * @param onRefreshTokenRotated invoked (off-mutex) with the session's refresh token after every
 *   *applied* successful refresh/restore/sign-in, so the owner can persist a rotated token.
 */
@OptIn(ExperimentalTime::class)
internal class FirebaseIdTokenProvider(
    private val httpClient: HttpClient,
    private val apiKey: String,
    private val scope: CoroutineScope,
    private val now: () -> Long = { Clock.System.now().toEpochMilliseconds() },
    private val onRefreshTokenRotated: suspend (String) -> Unit = {},
) {
    private data class Session(
        val idToken: String,
        val refreshToken: String,
        val expiresAtMs: Long,
    )

    /** Typed outcome of [getToken] — see [com.chriscartland.batterybutler.datanetwork.LabsTokenResult] for the caller-facing mapping. */
    sealed interface TokenOutcome {
        /** A usable token; [servedFromCache] is false when a refresh round-trip produced it on this call. */
        data class Token(
            val idToken: String,
            val servedFromCache: Boolean,
        ) : TokenOutcome

        /** No in-memory session (never signed in here, or signed out). */
        data object NoSession : TokenOutcome

        /** The refresh token was authoritatively rejected; the in-memory session was cleared. */
        data object Invalid : TokenOutcome

        /** The refresh failed transiently (network/5xx); the session is kept for a later retry. */
        data object TransientFailure : TokenOutcome
    }

    private val log = Logger.withTag("FirebaseIdTokenProvider")
    private val mutex = Mutex()
    private var session: Session? = null

    /** Bumped whenever the session identity changes outside a refresh (sign-out, new sign-in), so a stale refresh can't apply. */
    private var sessionEpoch = 0
    private var inFlightRefresh: Deferred<RefreshOutcome>? = null
    private var proactiveRefreshEnabled = false
    private var proactiveJob: Job? = null

    /**
     * Exchange a freshly-obtained Google ID token for a Labs session. Interactive — call once after
     * Google Sign-In; [getToken] keeps it fresh afterwards. The success value carries the identity
     * the Labs backend minted (`localId` = the Firebase uid, plus the account email) so the caller
     * can key the signed-in user on the uid the backend actually authorizes with.
     */
    suspend fun signInWithGoogle(googleIdToken: String): Result<LabsSignInIdentity, AuthError> {
        if (apiKey.isBlank()) {
            return Result.Error(AuthError.Configuration.NotConfigured())
        }
        return try {
            val response = httpClient.post(SIGN_IN_WITH_IDP_URL) {
                parameter("key", apiKey)
                contentType(ContentType.Application.Json)
                setBody(
                    SignInWithIdpRequest(postBody = "id_token=$googleIdToken&providerId=google.com"),
                )
            }
            if (!response.status.isSuccess()) {
                return Result.Error(
                    AuthError.SignIn.Failed(cause = "signInWithIdp HTTP ${response.status.value}"),
                )
            }
            val body = response.body<SignInWithIdpResponse>()
            if (body.idToken.isBlank() || body.refreshToken.isBlank()) {
                return Result.Error(AuthError.SignIn.Failed(cause = "signInWithIdp returned no token"))
            }
            mutex.withLock {
                sessionEpoch++
                inFlightRefresh = null // a stale refresh must not clobber the brand-new session
                session = body.toSession()
                rescheduleProactiveLocked()
            }
            onRefreshTokenRotated(body.refreshToken)
            Result.Success(
                LabsSignInIdentity(
                    firebaseUid = body.localId.ifBlank { null },
                    email = body.email.ifBlank { null },
                ),
            )
        } catch (e: CancellationException) {
            throw e
        } catch (e: Exception) {
            Result.Error(AuthError.SignIn.NetworkError(cause = e.message))
        }
    }

    /**
     * The current Labs Firebase ID token as a typed outcome, refreshing (single-flight) when the
     * token is within [EXPIRY_BUFFER_MS] of expiry or [forceRefresh] is set. The mutex is never
     * held across the refresh network call; concurrent callers await one shared refresh.
     */
    suspend fun getToken(forceRefresh: Boolean = false): TokenOutcome {
        val refreshJob = mutex.withLock {
            val current = session ?: return TokenOutcome.NoSession
            if (!forceRefresh && now() < current.expiresAtMs - EXPIRY_BUFFER_MS) {
                return TokenOutcome.Token(current.idToken, servedFromCache = true)
            }
            startOrJoinRefreshLocked(current.refreshToken)
        }
        return when (val outcome = refreshJob.await()) {
            is RefreshOutcome.Success -> TokenOutcome.Token(outcome.session.idToken, servedFromCache = false)
            RefreshOutcome.Invalid -> TokenOutcome.Invalid
            RefreshOutcome.Transient -> TokenOutcome.TransientFailure
        }
    }

    /**
     * The current token or null — legacy convenience for callers that only need a best-effort
     * token (e.g. Settings' "Copy Labs ID Token"). Sync paths use [getToken], which does not
     * conflate "no session" with "transient refresh failure".
     */
    suspend fun getIdToken(): String? = (getToken() as? TokenOutcome.Token)?.idToken

    /**
     * Restore a session non-interactively from a persisted [refreshToken] (e.g. right after
     * process start), without any Google/Credential Manager involvement -- a plain network call,
     * so unlike [com.chriscartland.batterybutler.datanetwork.auth.GoogleSignInBridge]'s silent
     * methods it carries no OS-UI risk. Distinguishes an **authoritative** rejection (the refresh
     * token itself is invalid/revoked -- [AuthError.Token.Invalid], the caller should stop treating
     * the user as signed in) from a **transient** failure (network unreachable, server error --
     * [AuthError.SignIn.NetworkError], safe to leave the existing believed-signed-in state alone
     * and retry later). Shares the single-flight refresh with [getToken], so a concurrent restore
     * and token read perform one securetoken call between them.
     */
    suspend fun restoreSession(refreshToken: String): Result<Unit, AuthError> {
        if (apiKey.isBlank()) return Result.Error(AuthError.Configuration.NotConfigured())
        val refreshJob = mutex.withLock { startOrJoinRefreshLocked(refreshToken) }
        return when (refreshJob.await()) {
            is RefreshOutcome.Success -> Result.Success(Unit)
            RefreshOutcome.Invalid -> Result.Error(AuthError.Token.Invalid(message = "Labs refresh token rejected"))
            RefreshOutcome.Transient -> Result.Error(AuthError.SignIn.NetworkError())
        }
    }

    /**
     * The current session's refresh token (kept up to date across refreshes, in case Firebase
     * rotates it), or null if not signed in. Read by [com.chriscartland.batterybutler
     * .datanetwork.DefaultLabsAuthGateway] for persistence -- see
     * [com.chriscartland.batterybutler.domain.repository.LabsRefreshTokenPersistence].
     */
    suspend fun currentRefreshToken(): String? = mutex.withLock { session?.refreshToken }

    /** Clear the session (e.g. on sign-out): drops the token, detaches any in-flight refresh, cancels proactive refresh. */
    suspend fun signOut() =
        mutex.withLock {
            sessionEpoch++
            inFlightRefresh = null // detached: a landing refresh sees the epoch mismatch and applies nothing
            session = null
            rescheduleProactiveLocked()
        }

    /**
     * Enables/disables the background proactive refresh for this provider. The gateway enables it
     * only for the currently-selected environment's provider, so an inactive environment's session
     * doesn't keep the radio busy. Idempotent.
     */
    suspend fun setProactiveRefresh(enabled: Boolean) =
        mutex.withLock {
            if (proactiveRefreshEnabled == enabled) return@withLock
            proactiveRefreshEnabled = enabled
            rescheduleProactiveLocked()
        }

    /**
     * (Re)schedules the proactive-refresh task from the current session/flag. Must be called while
     * holding [mutex]. Cancels any previously scheduled task; schedules nothing when disabled or
     * signed out.
     */
    private fun rescheduleProactiveLocked() {
        proactiveJob?.cancel()
        proactiveJob = null
        val current = session
        if (!proactiveRefreshEnabled || current == null) return
        val waitMs = (current.expiresAtMs - EXPIRY_BUFFER_MS - now()).coerceAtLeast(0L)
        proactiveJob = scope.launch {
            delay(waitMs)
            proactiveRefreshOnce()
        }
    }

    /**
     * One proactive refresh attempt. On success the refresh-apply path reschedules the next run
     * (and cancels this task); on a transient failure this retries after [PROACTIVE_RETRY_MS];
     * on no-session/invalid it simply stops (sign-out/invalidation already tore scheduling down).
     */
    private suspend fun proactiveRefreshOnce() {
        when (getToken()) {
            is TokenOutcome.Token, TokenOutcome.NoSession, TokenOutcome.Invalid -> {
                Unit
            }

            TokenOutcome.TransientFailure -> {
                mutex.withLock {
                    if (!proactiveRefreshEnabled || session == null) return@withLock
                    proactiveJob = scope.launch {
                        delay(PROACTIVE_RETRY_MS)
                        proactiveRefreshOnce()
                    }
                }
            }
        }
    }

    private sealed interface RefreshOutcome {
        data class Success(
            val session: Session,
        ) : RefreshOutcome

        /** HTTP 4xx: the refresh token itself was rejected -- authoritative, not worth retrying. */
        data object Invalid : RefreshOutcome

        /** Network exception or non-4xx failure: worth retrying later, doesn't imply revocation. */
        data object Transient : RefreshOutcome
    }

    /**
     * Starts a refresh on [scope], or joins the one already in flight (single-flight: concurrent
     * callers all await the same [Deferred]). Must be called while holding [mutex]. The result is
     * applied to [session] exactly once, inside the deferred, guarded by the session epoch — a
     * refresh that lands after a sign-out or a new sign-in applies nothing.
     */
    private fun startOrJoinRefreshLocked(refreshToken: String): Deferred<RefreshOutcome> {
        inFlightRefresh?.let { return it }
        val epochAtStart = sessionEpoch
        var jobRef: Deferred<RefreshOutcome>? = null
        val job = scope.async {
            val outcome = refresh(refreshToken)
            var rotatedToken: String? = null
            mutex.withLock {
                if (inFlightRefresh === jobRef) inFlightRefresh = null
                if (sessionEpoch == epochAtStart) {
                    when (outcome) {
                        is RefreshOutcome.Success -> {
                            session = outcome.session
                            rotatedToken = outcome.session.refreshToken
                            rescheduleProactiveLocked()
                        }

                        RefreshOutcome.Invalid -> {
                            session = null
                            rescheduleProactiveLocked()
                        }

                        RefreshOutcome.Transient -> {
                            Unit
                        }
                    }
                }
            }
            rotatedToken?.let { onRefreshTokenRotated(it) }
            outcome
        }
        jobRef = job
        inFlightRefresh = job
        return job
    }

    private suspend fun refresh(refreshToken: String): RefreshOutcome {
        if (apiKey.isBlank()) return RefreshOutcome.Transient
        return try {
            val response = httpClient.post(SECURE_TOKEN_URL) {
                parameter("key", apiKey)
                contentType(ContentType.Application.Json)
                setBody(RefreshTokenRequest(refreshToken = refreshToken))
            }
            if (response.status.value in INVALID_STATUS_RANGE) {
                log.i { "Labs Firebase refresh token rejected: HTTP ${response.status.value}" }
                return RefreshOutcome.Invalid
            }
            if (!response.status.isSuccess()) return RefreshOutcome.Transient
            val body = response.body<RefreshTokenResponse>()
            if (body.idToken.isBlank()) {
                RefreshOutcome.Transient
            } else {
                RefreshOutcome.Success(
                    Session(
                        idToken = body.idToken,
                        refreshToken = body.refreshToken.ifBlank { refreshToken },
                        expiresAtMs = expiresAtFrom(body.expiresIn),
                    ),
                )
            }
        } catch (e: CancellationException) {
            throw e
        } catch (e: Exception) {
            log.w(e) { "Labs Firebase token refresh failed" }
            RefreshOutcome.Transient
        }
    }

    private fun SignInWithIdpResponse.toSession() = Session(idToken = idToken, refreshToken = refreshToken, expiresAtMs = expiresAtFrom(expiresIn))

    private fun expiresAtFrom(expiresInSeconds: String): Long = now() + (expiresInSeconds.toLongOrNull() ?: 0L) * MILLIS_PER_SECOND

    internal companion object {
        const val SIGN_IN_WITH_IDP_URL =
            "https://identitytoolkit.googleapis.com/v1/accounts:signInWithIdp"
        const val SECURE_TOKEN_URL = "https://securetoken.googleapis.com/v1/token"

        /**
         * How early before expiry a token is treated as stale. 5 minutes (was 60s), so the
         * proactive-refresh task has a comfortable window to renew off the request path and a
         * user-facing request essentially never pays the securetoken round-trip itself.
         */
        const val EXPIRY_BUFFER_MS = 300_000L

        /** Retry delay for the proactive-refresh task after a transient refresh failure. */
        const val PROACTIVE_RETRY_MS = 60_000L
        const val MILLIS_PER_SECOND = 1_000L
        val INVALID_STATUS_RANGE = 400..499
    }
}
