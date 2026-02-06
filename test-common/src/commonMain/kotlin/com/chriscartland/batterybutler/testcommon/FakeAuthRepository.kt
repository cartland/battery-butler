package com.chriscartland.batterybutler.testcommon

import com.chriscartland.batterybutler.domain.model.AuthError
import com.chriscartland.batterybutler.domain.model.AuthState
import com.chriscartland.batterybutler.domain.model.Result
import com.chriscartland.batterybutler.domain.model.User
import com.chriscartland.batterybutler.domain.repository.AuthRepository
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.map

/**
 * Fake implementation of [AuthRepository] for testing.
 *
 * Provides controllable auth behavior for unit tests:
 * - Configure sign-in results via [signInResult]
 * - Track method calls via [signInCallCount], [signOutCallCount]
 * - Directly manipulate state via [setAuthState]
 *
 * Example usage:
 * ```kotlin
 * val repo = FakeAuthRepository()
 * repo.signInResult = Result.Success(testUser)
 * repo.signInWithGoogle()
 * assertEquals(AuthState.Authenticated(testUser), repo.authState.value)
 * ```
 */
class FakeAuthRepository : AuthRepository {
    private val _authState = MutableStateFlow<AuthState>(AuthState.Unknown)

    override val authState: StateFlow<AuthState> = _authState

    override val currentUser: Flow<User?> = _authState.map { state ->
        (state as? AuthState.Authenticated)?.user
    }

    /** Configure whether sign-in is available. Default: true */
    var isConfigured: Boolean = true

    /** Configure the result returned by [signInWithGoogle]. */
    var signInResult: Result<User, AuthError> = Result.Error(
        AuthError.Configuration.NotConfigured(),
    )

    /** Configure the result returned by [refreshToken]. */
    var refreshTokenResult: Result<Unit, AuthError> = Result.Success(Unit)

    /** Count of [signInWithGoogle] calls for verification. */
    var signInCallCount: Int = 0
        private set

    /** Count of [signOut] calls for verification. */
    var signOutCallCount: Int = 0
        private set

    /** Count of [clearError] calls for verification. */
    var clearErrorCallCount: Int = 0
        private set

    /** Directly set the auth state for testing. */
    fun setAuthState(state: AuthState) {
        _authState.value = state
    }

    override fun isSignInAvailable(): Boolean = isConfigured

    override suspend fun signInWithGoogle(): Result<User, AuthError> {
        signInCallCount++
        _authState.value = AuthState.Authenticating

        return when (val result = signInResult) {
            is Result.Success -> {
                _authState.value = AuthState.Authenticated(result.data)
                result
            }
            is Result.Error -> {
                _authState.value = AuthState.Failed(result.error as AuthError)
                result
            }
        }
    }

    override suspend fun signOut() {
        signOutCallCount++
        _authState.value = AuthState.Unauthenticated
    }

    override suspend fun refreshToken(): Result<Unit, AuthError> = refreshTokenResult

    override fun clearError() {
        clearErrorCallCount++
        if (_authState.value is AuthState.Failed) {
            _authState.value = AuthState.Unauthenticated
        }
    }
}
