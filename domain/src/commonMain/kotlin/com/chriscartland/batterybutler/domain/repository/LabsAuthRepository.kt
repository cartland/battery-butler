package com.chriscartland.batterybutler.domain.repository

import com.chriscartland.batterybutler.domain.model.AuthError
import com.chriscartland.batterybutler.domain.model.AuthState
import com.chriscartland.batterybutler.domain.model.NetworkModeKeyedState
import com.chriscartland.batterybutler.domain.model.Result
import com.chriscartland.batterybutler.domain.model.User
import kotlinx.coroutines.flow.Flow

/**
 * Authentication for the **Labs REST backend** (`NetworkMode.LabsStaging` / `LabsProd`).
 *
 * Separate from [AuthRepository] (which authenticates battery-butler's own gRPC backend). The Labs
 * flow is: Google Sign-In against the *Labs* OAuth client → exchange the Google ID token for a Labs
 * Firebase ID token (held by the Labs auth gateway) → subsequent Labs `/sync` calls carry it.
 *
 * [labsAuthState] reuses [AuthState] for UI parity with the own-backend account card. The Labs
 * session is in-memory (not persisted), so it starts [AuthState.Unauthenticated] every launch.
 * It's keyed per network mode (see [NetworkModeKeyedState]): switching between Labs staging and
 * prod reflects each environment's *own* status, never a stale value carried over from whichever
 * one was previously selected.
 */
interface LabsAuthRepository {
    /** Current Labs auth state for the *currently selected* network mode. Starts [AuthState.Unauthenticated]. */
    val labsAuthState: Flow<AuthState>

    /**
     * Sign in to the Labs backend for the currently-selected Labs network mode. Picks the matching
     * Labs OAuth client (staging vs prod), runs Google Sign-In against it, and exchanges the token.
     * Fails if the current mode isn't a Labs mode or the client isn't configured.
     */
    suspend fun signInToLabs(): Result<User, AuthError>

    /** Clear the Labs session. */
    suspend fun signOutLabs()

    /** If [labsAuthState] is [AuthState.Failed], transition back to [AuthState.Unauthenticated]. */
    suspend fun clearError()

    /**
     * The current Labs Firebase ID token, refreshing it first if it's near expiry; null if not
     * signed in. This is a live, short-lived credential (about an hour) for the currently-selected
     * Labs environment — treat it like a password. Exposed so a user can copy it into a trusted
     * external tool (e.g. the Labs CLI) without that tool needing its own OAuth flow.
     */
    suspend fun getLabsIdToken(): String?
}
