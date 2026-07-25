package com.chriscartland.batterybutler.domain.repository

import com.chriscartland.batterybutler.domain.model.AuthError
import com.chriscartland.batterybutler.domain.model.AuthState
import com.chriscartland.batterybutler.domain.model.DataModeKeyedState
import com.chriscartland.batterybutler.domain.model.LabsSessionRestoreResult
import com.chriscartland.batterybutler.domain.model.Result
import com.chriscartland.batterybutler.domain.model.User
import kotlinx.coroutines.flow.Flow

/**
 * Authentication for the **Labs REST backend** (`DataMode.LabsStaging` / `LabsProd`).
 *
 * Separate from [AuthRepository] (which authenticates battery-butler's own gRPC backend). The Labs
 * flow is: Google Sign-In against the *Labs* OAuth client → exchange the Google ID token for a Labs
 * Firebase ID token (held by the Labs auth gateway) → subsequent Labs `/sync` calls carry it.
 *
 * [labsAuthState] reuses [AuthState] for UI parity with the own-backend account card. A lightweight
 * "believed signed in" belief survives process restarts (see `LabsSessionStorage`), and the real
 * session is restored non-interactively from a persisted refresh token where possible (see
 * `LabsRefreshTokenPersistence`) -- so [AuthState.Authenticated] can carry across a relaunch rather
 * than starting [AuthState.Unauthenticated] every time. It's keyed per data mode (see
 * [DataModeKeyedState]): switching between Labs staging and prod reflects each environment's *own*
 * status, never a stale value carried over from whichever one was previously selected.
 */
interface LabsAuthRepository {
    /** Current Labs auth state for the *currently selected* data mode. Starts [AuthState.Unauthenticated]. */
    val labsAuthState: Flow<AuthState>

    /**
     * Sign in to the Labs backend for the currently-selected Labs data mode. Picks the matching
     * Labs OAuth client (staging vs prod), runs Google Sign-In against it, and exchanges the token.
     * Fails if the current mode isn't a Labs mode or the client isn't configured.
     */
    suspend fun signInToLabs(): Result<User, AuthError>

    /**
     * Suspends until the *currently selected* environment's persisted-session restore has
     * resolved, triggering the restore if it hasn't started yet. This is the cold-start gate the
     * sync layer awaits before firing its first Labs request, so a believed-signed-in launch
     * doesn't churn out "sign in required" while the non-interactive restore
     * ([com.chriscartland.batterybutler.domain.repository.LabsRefreshTokenPersistence] ->
     * securetoken) is still in flight. Resolution is per environment and sticky: once resolved
     * (any [LabsSessionRestoreResult]) this returns immediately. A transient restore failure
     * resolves as [LabsSessionRestoreResult.TRANSIENT_FAILURE] rather than blocking — sync then
     * proceeds and surfaces network errors, never a spurious auth prompt.
     */
    suspend fun awaitLabsSessionRestore(): LabsSessionRestoreResult

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
