package com.chriscartland.batterybutler.datanetwork

import com.chriscartland.batterybutler.domain.model.AuthError
import com.chriscartland.batterybutler.domain.model.Result
import kotlinx.coroutines.flow.Flow

/**
 * App-facing seam for the Labs REST session.
 *
 * The Labs `/sync` calls carry a Firebase ID token (Workstream D). That token comes from a single,
 * in-memory session that must be **shared** between the interactive sign-in and every sync call —
 * so this gateway is provided as a DI **singleton** ([DefaultLabsAuthGateway]) and is the one place
 * that owns the session:
 *
 *  - [signInToLabsWithGoogle] — the one interactive step (`bb-labs-signin`): exchange a Google ID
 *    token (minted for the **Labs** OAuth client) for a Labs session. Call once after Google
 *    Sign-In. Success carries the [LabsSignInIdentity] the backend minted (Firebase uid + email)
 *    so the caller keys the signed-in user on the uid the backend authorizes with.
 *  - [getLabsToken] (from [LabsSyncTokenSource]) — the `Authorization: Bearer` source for
 *    [com.chriscartland.batterybutler.datanetwork.rest.RestRemoteDataSource], as a typed
 *    [LabsTokenResult] so "no session" and "transient refresh failure" stay distinguishable;
 *    refreshed silently, restored on demand from the persisted refresh token when the in-memory
 *    session is gone. [getLabsIdToken] remains the nullable convenience for non-sync callers
 *    (e.g. Settings' token copy).
 *  - [sessionInvalidations] — emits when a session is authoritatively rejected (a forced refresh
 *    reports the refresh token invalid, or [reportSessionRejected] delivers a terminal 401): the
 *    gateway clears the in-memory session + persisted refresh token, then emits so the auth
 *    repository can flip that environment's state to session-expired. Local device data is
 *    never touched here.
 *  - [restoreSession] — rebuild a session from a *persisted* refresh token (e.g. right after
 *    process start), with no Google/Credential Manager involvement. See
 *    `bb-labs-refresh-token-persistence` in TODO.md.
 *  - [signOutLabs] — clears the session (in-memory and persisted).
 *
 * Why a gateway rather than letting [DelegatingRemoteDataSource] hold the provider: the session
 * holder must be the singleton. [DelegatingRemoteDataSource] is unscoped, so the shared session
 * lives here instead — a sign-in and a sync then always hit the same session regardless of how
 * many delegating-data-source instances exist.
 */
interface LabsAuthGateway : LabsSyncTokenSource {
    suspend fun signInToLabsWithGoogle(googleIdToken: String): Result<LabsSignInIdentity, AuthError>

    /**
     * The current token or null — convenience for callers that only need a best-effort token
     * (e.g. Settings' "Copy Labs ID Token"). Sync paths use [getLabsToken] instead, which does
     * not conflate "no session" with "transient refresh failure".
     */
    suspend fun getLabsIdToken(): String?

    /**
     * Reactive session-loss events, one per authoritative rejection (see
     * [LabsSessionInvalidation]). By the time an event is observed the gateway has already
     * cleared the in-memory session and the persisted refresh token for that environment.
     */
    val sessionInvalidations: Flow<LabsSessionInvalidation>

    suspend fun signOutLabs()

    /**
     * Restore a session for the currently-selected Labs environment from its persisted refresh
     * token, via a plain network call — no Google/Credential Manager involvement, so (unlike
     * [com.chriscartland.batterybutler.datanetwork.auth.GoogleSignInBridge]'s silent methods) this
     * carries no OS-UI risk and is safe to call on every process start. [AuthError.Token.Invalid]
     * means the refresh token was actually rejected (an authoritative "signed out" signal); other
     * errors (no persisted token, network unreachable, unconfigured) are transient/expected and
     * should be left alone rather than treated as a sign-out.
     */
    suspend fun restoreSession(): Result<Unit, AuthError>
}
