package com.chriscartland.batterybutler.domain.repository

import com.chriscartland.batterybutler.domain.model.AuthError
import com.chriscartland.batterybutler.domain.model.AuthState
import com.chriscartland.batterybutler.domain.model.Result
import com.chriscartland.batterybutler.domain.model.User
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.StateFlow

/**
 * Repository interface for authentication operations.
 *
 * Implementations handle:
 * - OAuth provider integration (Google Sign-In)
 * - Token storage and refresh
 * - Session management
 *
 * ## Architecture
 * - [authState] is the source of truth for authentication status
 * - Sign-in operations update [authState] as they progress
 * - Errors are surfaced through [AuthState.Failed]
 *
 * ## Flow Semantics
 * - [authState] emits immediately with current state
 * - [currentUser] derives from [authState], emitting User or null
 */
interface AuthRepository {
    /**
     * Current authentication state.
     * Observe this to update UI based on auth status.
     */
    val authState: StateFlow<AuthState>

    /**
     * Flow of the currently authenticated user, or null if not authenticated.
     * Convenience accessor derived from [authState].
     */
    val currentUser: Flow<User?>

    /**
     * Returns true if sign-in is available (OAuth configured).
     */
    fun isSignInAvailable(): Boolean

    /**
     * Initiates Google Sign-In flow.
     *
     * Updates [authState] through the flow:
     * - Sets to [AuthState.Authenticating] when starting
     * - Sets to [AuthState.Authenticated] on success
     * - Sets to [AuthState.Failed] on failure
     *
     * @return [Result.Success] with [User] on success, [Result.Error] with [AuthError] on failure.
     */
    suspend fun signInWithGoogle(): Result<User, AuthError>

    /**
     * Signs out the current user.
     *
     * Clears stored tokens and sets [authState] to [AuthState.Unauthenticated].
     */
    suspend fun signOut()

    /**
     * Attempts to refresh the current session token.
     *
     * @return [Result.Success] on successful refresh, [Result.Error] if refresh fails.
     */
    suspend fun refreshToken(): Result<Unit, AuthError>

    /**
     * Clears the current error state.
     *
     * If [authState] is [AuthState.Failed], transitions to [AuthState.Unauthenticated].
     * No-op if not in failed state.
     */
    fun clearError()
}
