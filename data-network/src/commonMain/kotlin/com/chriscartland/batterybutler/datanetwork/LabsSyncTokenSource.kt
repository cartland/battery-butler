package com.chriscartland.batterybutler.datanetwork

import com.chriscartland.batterybutler.domain.model.SyncAuthReason

/**
 * Result of asking the Labs auth layer for a Bearer token.
 *
 * Replaces the old `suspend () -> String?` token provider, whose `null` conflated two states
 * with opposite user-facing meanings: "there is no session — the user must sign in" and "there
 * is a session but refreshing it just failed on a flaky network". The first is an auth problem;
 * the second must surface as a *network* problem (a spurious "sign in required" on a bad
 * connection was exactly the failure this fixes).
 */
sealed interface LabsTokenResult {
    /**
     * A usable ID token.
     *
     * @property idToken the Labs Firebase ID token for the `Authorization: Bearer` header.
     * @property servedFromCache true when the token was returned from the in-memory session
     *   without a refresh round-trip on this call — i.e. the session *locally looked unexpired*.
     *   The retry policy uses this: a 401 with an unknown reason on a cache-served token earns
     *   one forced refresh + retry (maybe the server knows something we don't), while the same
     *   401 on a token we *just* minted is authoritative — retrying with another fresh token
     *   can't do better.
     */
    data class Token(
        val idToken: String,
        val servedFromCache: Boolean,
    ) : LabsTokenResult

    /** No session and no persisted refresh token: the user must sign in. Definitive. */
    data object NoSession : LabsTokenResult

    /**
     * The refresh token was authoritatively rejected while obtaining the token. The gateway has
     * already run the session-loss reaction (cleared the session + persisted credentials and
     * emitted [LabsSessionInvalidation]); callers just report "sign in required".
     */
    data object SessionInvalidated : LabsTokenResult

    /**
     * A session (or a persisted refresh token) exists, but the refresh/restore attempt failed
     * transiently — network unreachable, server 5xx. NOT an auth failure: callers must surface
     * this on the network-failure path, never as "sign in required".
     */
    data object TransientFailure : LabsTokenResult
}

/**
 * A Labs session was authoritatively rejected and torn down (reactively — the user did nothing).
 *
 * Emitted by [LabsAuthGateway.sessionInvalidations] after the gateway has cleared the in-memory
 * session and the persisted refresh token for [environmentKey]. The auth repository reacts by
 * flipping that environment's auth state to session-expired and clearing the believed-signed-in
 * user. Local device data is deliberately NOT touched — only an explicit sign-out clears it.
 *
 * @property environmentKey the per-environment partition key (see `apiKeyForMode`), carried so
 *   the reaction targets the right environment even if the user has since switched modes.
 * @property reason the parsed cause, advisory detail for logging/copy.
 */
data class LabsSessionInvalidation(
    val environmentKey: String,
    val reason: SyncAuthReason,
)

/**
 * The narrow seam [com.chriscartland.batterybutler.datanetwork.rest.RestRemoteDataSource] needs
 * from the auth layer: obtain a Bearer token (optionally force-refreshed for the retry-once
 * policy) and report a terminal server-side session rejection. [LabsAuthGateway] extends this;
 * tests fake just these two members.
 */
interface LabsSyncTokenSource {
    /**
     * The current environment's Labs token. With [forceRefresh] the in-memory token is discarded
     * and a refresh round-trip is forced (single-flight: concurrent callers share one refresh).
     * When no in-memory session exists but a persisted refresh token does, this restores the
     * session on demand — so a caller is never told [LabsTokenResult.NoSession] while a
     * restorable session sits in persistence.
     */
    suspend fun getLabsToken(forceRefresh: Boolean = false): LabsTokenResult

    /**
     * A Labs request was refused as unauthenticated *terminally* — after the retry-once policy
     * was exhausted (or judged futile: a freshly-minted token was rejected). The gateway runs
     * the session-loss reaction: clear the in-memory session + persisted refresh token for the
     * current environment and emit [LabsSessionInvalidation].
     */
    suspend fun reportSessionRejected(reason: SyncAuthReason)
}
