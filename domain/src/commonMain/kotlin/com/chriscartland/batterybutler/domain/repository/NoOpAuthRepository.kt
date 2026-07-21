package com.chriscartland.batterybutler.domain.repository

import com.chriscartland.batterybutler.domain.model.AuthError
import com.chriscartland.batterybutler.domain.model.AuthState
import com.chriscartland.batterybutler.domain.model.Result
import com.chriscartland.batterybutler.domain.model.User
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.flowOf

data object NoOpAuthRepository : AuthRepository {
    override val authState: StateFlow<AuthState> =
        MutableStateFlow(AuthState.Unauthenticated())
    override val currentUser: Flow<User?> = flowOf(null)

    override fun isSignInAvailable(): Boolean = false

    override suspend fun signInWithGoogle(): Result<User, AuthError> =
        Result.Error(
            AuthError.Configuration.NotConfigured(
                message = "Not available",
                cause = "Auth not configured",
            ),
        )

    override suspend fun signOut() {}

    override suspend fun refreshToken(): Result<Unit, AuthError> =
        Result.Error(
            AuthError.Token.Expired(
                message = "Not available",
                cause = "Auth not configured",
            ),
        )

    override fun clearError() {}
}
