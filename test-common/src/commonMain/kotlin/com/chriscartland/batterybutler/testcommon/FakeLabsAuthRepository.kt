package com.chriscartland.batterybutler.testcommon

import com.chriscartland.batterybutler.domain.model.AuthError
import com.chriscartland.batterybutler.domain.model.AuthState
import com.chriscartland.batterybutler.domain.model.Result
import com.chriscartland.batterybutler.domain.model.User
import com.chriscartland.batterybutler.domain.repository.LabsAuthRepository
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow

/**
 * Controllable [LabsAuthRepository] fake for tests. [signInToLabs] applies [signInResult] to the
 * state ([AuthState.Authenticated] on success, [AuthState.Failed] on error) and records call counts.
 */
class FakeLabsAuthRepository : LabsAuthRepository {
    private val _labsAuthState = MutableStateFlow<AuthState>(AuthState.Unauthenticated)
    override val labsAuthState: StateFlow<AuthState> = _labsAuthState.asStateFlow()

    var signInResult: Result<User, AuthError> =
        Result.Success(
            User(id = "labs-user", email = "labs@example.com", displayName = "Labs User", photoUrl = null),
        )
    var signInCount = 0
    var signOutCount = 0
    var idToken: String? = null

    override suspend fun signInToLabs(): Result<User, AuthError> {
        signInCount++
        when (val result = signInResult) {
            is Result.Success -> _labsAuthState.value = AuthState.Authenticated(result.data)
            is Result.Error -> _labsAuthState.value = AuthState.Failed(result.error)
        }
        return signInResult
    }

    override suspend fun signOutLabs() {
        signOutCount++
        _labsAuthState.value = AuthState.Unauthenticated
    }

    override suspend fun clearError() {
        if (_labsAuthState.value is AuthState.Failed) {
            _labsAuthState.value = AuthState.Unauthenticated
        }
    }

    /** Force a state directly (e.g. to simulate already-signed-in). */
    fun setLabsAuthState(state: AuthState) {
        _labsAuthState.value = state
    }

    override suspend fun getLabsIdToken(): String? = idToken
}
