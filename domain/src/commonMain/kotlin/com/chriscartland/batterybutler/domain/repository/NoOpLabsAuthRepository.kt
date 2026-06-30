package com.chriscartland.batterybutler.domain.repository

import com.chriscartland.batterybutler.domain.model.AuthError
import com.chriscartland.batterybutler.domain.model.AuthState
import com.chriscartland.batterybutler.domain.model.Result
import com.chriscartland.batterybutler.domain.model.User
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow

/**
 * No-op [LabsAuthRepository] for platforms where Labs sign-in isn't wired (iOS — it needs its own
 * iOS OAuth client + URL scheme; desktop-first). Always unauthenticated; sign-in reports "not
 * available" rather than appearing to work.
 */
data object NoOpLabsAuthRepository : LabsAuthRepository {
    override val labsAuthState: StateFlow<AuthState> =
        MutableStateFlow(AuthState.Unauthenticated)

    override suspend fun signInToLabs(): Result<User, AuthError> =
        Result.Error(
            AuthError.Configuration.NotConfigured(
                message = "Labs sign-in not available",
                cause = "Labs sign-in is not supported on this platform",
            ),
        )

    override suspend fun signOutLabs() {}

    override fun clearError() {}
}
