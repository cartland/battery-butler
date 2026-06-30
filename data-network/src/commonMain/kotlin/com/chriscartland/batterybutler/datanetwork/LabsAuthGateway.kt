package com.chriscartland.batterybutler.datanetwork

import com.chriscartland.batterybutler.domain.model.AuthError
import com.chriscartland.batterybutler.domain.model.Result

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
 *    Sign-In.
 *  - [getLabsIdToken] — the `Authorization: Bearer` source for [com.chriscartland.batterybutler
 *    .datanetwork.rest.RestRemoteDataSource]; `null` until sign-in (or when unconfigured),
 *    refreshed silently afterwards.
 *  - [signOutLabs] — clears the session.
 *
 * Why a gateway rather than letting [DelegatingRemoteDataSource] hold the provider: the session
 * holder must be the singleton. [DelegatingRemoteDataSource] is unscoped, so the shared session
 * lives here instead — a sign-in and a sync then always hit the same session regardless of how
 * many delegating-data-source instances exist.
 */
interface LabsAuthGateway {
    suspend fun signInToLabsWithGoogle(googleIdToken: String): Result<Unit, AuthError>

    suspend fun getLabsIdToken(): String?

    suspend fun signOutLabs()
}
