package com.chriscartland.batterybutler.viewmodel.login

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.chriscartland.batterybutler.domain.model.AuthState
import com.chriscartland.batterybutler.domain.repository.AuthRepository
import com.chriscartland.batterybutler.viewmodel.defaultWhileSubscribed
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.launch
import me.tatarka.inject.annotations.Inject

/**
 * ViewModel for the Login screen.
 *
 * Handles Google Sign-In flow and authentication state management.
 */
@Inject
class LoginViewModel(
    private val authRepository: AuthRepository,
) : ViewModel() {
    /**
     * Current authentication state.
     * UI observes this to show appropriate content:
     * - [AuthState.Unknown]: Loading indicator
     * - [AuthState.Unauthenticated]: Sign-in options
     * - [AuthState.Authenticating]: Loading with disabled buttons
     * - [AuthState.Authenticated]: Navigate to main screen
     * - [AuthState.Failed]: Error message with retry option
     */
    val authState: StateFlow<AuthState> = authRepository.authState
        .stateIn(
            scope = viewModelScope,
            started = defaultWhileSubscribed(),
            initialValue = AuthState.Unknown,
        )

    /**
     * Returns true if Google Sign-In is available (configured).
     */
    val isSignInAvailable: Boolean
        get() = authRepository.isSignInAvailable()

    /**
     * Initiates Google Sign-In flow.
     */
    fun signInWithGoogle() {
        viewModelScope.launch {
            authRepository.signInWithGoogle()
        }
    }

    /**
     * Clears the current error state, returning to unauthenticated.
     * Call this when user dismisses an error dialog.
     */
    fun dismissError() {
        authRepository.clearError()
    }
}
