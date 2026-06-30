package com.chriscartland.batterybutler.domain.repository

import com.chriscartland.batterybutler.domain.model.AuthError
import com.chriscartland.batterybutler.domain.model.AuthState
import com.chriscartland.batterybutler.domain.model.Result
import com.chriscartland.batterybutler.domain.model.User
import kotlinx.coroutines.flow.StateFlow

/**
 * Authentication for the **Labs REST backend** (`NetworkMode.LabsStaging` / `LabsProd`).
 *
 * Separate from [AuthRepository] (which authenticates battery-butler's own gRPC backend). The Labs
 * flow is: Google Sign-In against the *Labs* OAuth client → exchange the Google ID token for a Labs
 * Firebase ID token (held by the Labs auth gateway) → subsequent Labs `/sync` calls carry it.
 *
 * [labsAuthState] reuses [AuthState] for UI parity with the own-backend account card. The Labs
 * session is in-memory (not persisted), so it starts [AuthState.Unauthenticated] every launch.
 */
interface LabsAuthRepository {
    /** Current Labs auth state. Starts [AuthState.Unauthenticated]. */
    val labsAuthState: StateFlow<AuthState>

    /**
     * Sign in to the Labs backend for the currently-selected Labs network mode. Picks the matching
     * Labs OAuth client (staging vs prod), runs Google Sign-In against it, and exchanges the token.
     * Fails if the current mode isn't a Labs mode or the client isn't configured.
     */
    suspend fun signInToLabs(): Result<User, AuthError>

    /** Clear the Labs session. */
    suspend fun signOutLabs()

    /** If [labsAuthState] is [AuthState.Failed], transition back to [AuthState.Unauthenticated]. */
    fun clearError()
}
