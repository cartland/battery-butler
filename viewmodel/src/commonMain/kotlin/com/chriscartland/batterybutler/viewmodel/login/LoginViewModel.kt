package com.chriscartland.batterybutler.viewmodel.login

import com.chriscartland.batterybutler.domain.model.AuthState
import com.chriscartland.batterybutler.domain.model.NetworkMode
import com.chriscartland.batterybutler.domain.repository.AuthRepository
import com.chriscartland.batterybutler.domain.repository.LabsAuthRepository
import com.chriscartland.batterybutler.domain.repository.NetworkModeRepository
import com.chriscartland.batterybutler.usecase.SignInToLabsUseCase
import com.chriscartland.batterybutler.viewmodel.defaultWhileSubscribed
import com.chriscartland.batterybutler.viewmodel.safeStateIn
import com.rickclephas.kmp.observableviewmodel.ViewModel
import com.rickclephas.kmp.observableviewmodel.coroutineScope
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.flatMapLatest
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.launch
import me.tatarka.inject.annotations.Inject

/**
 * ViewModel for the Login screen.
 *
 * The front-door sign-in **follows the selected backend**: in a Labs ([NetworkMode.LabsStaging] /
 * [NetworkMode.LabsProd]) mode it drives the Labs Google sign-in, otherwise the app's own gRPC
 * account. This keeps the login tied to whichever backend the app is pointed at — the app defaults
 * to Labs (see `DataStoreNetworkModeRepository`), so a fresh install shows a prominent Labs sign-in.
 */
@OptIn(ExperimentalCoroutinesApi::class)
@Inject
class LoginViewModel(
    private val authRepository: AuthRepository,
    private val labsAuthRepository: LabsAuthRepository,
    private val networkModeRepository: NetworkModeRepository,
    private val signInToLabsUseCase: SignInToLabsUseCase,
) : ViewModel() {
    /**
     * True when the selected backend is a Labs (REST) mode — the front door then signs in to Labs.
     * Eagerly shared so [signInWithGoogle] / [dismissError] / [isSignInAvailable] can read `.value`.
     */
    val isLabsMode: StateFlow<Boolean> = networkModeRepository.networkMode
        .map { it is NetworkMode.LabsStaging || it is NetworkMode.LabsProd }
        .safeStateIn(
            viewModelScope = viewModelScope,
            started = SharingStarted.Eagerly,
            initialValue = false,
        )

    /**
     * Current authentication state, following the selected backend: the Labs session in a Labs mode,
     * otherwise the app's own (gRPC) account. UI observes this to show appropriate content:
     * - [AuthState.Unknown]: Loading indicator
     * - [AuthState.Unauthenticated]: Sign-in options
     * - [AuthState.Authenticating]: Loading with disabled buttons
     * - [AuthState.Authenticated]: Navigate to main screen
     * - [AuthState.Failed]: Error message with retry option
     */
    val authState: StateFlow<AuthState> = networkModeRepository.networkMode
        .flatMapLatest { mode ->
            if (mode is NetworkMode.LabsStaging || mode is NetworkMode.LabsProd) {
                labsAuthRepository.labsAuthState
            } else {
                authRepository.authState
            }
        }.safeStateIn(
            viewModelScope = viewModelScope,
            started = defaultWhileSubscribed(),
            initialValue = AuthState.Unknown,
        )

    /**
     * Whether a sign-in button should be offered. Always available in a Labs mode (the Labs client is
     * injected; misconfiguration surfaces as an error dialog); otherwise reflects the gRPC config.
     */
    val isSignInAvailable: Boolean
        get() = isLabsMode.value || authRepository.isSignInAvailable()

    /**
     * Initiates sign-in against the selected backend — Labs Google sign-in in a Labs mode, otherwise
     * the app's own gRPC sign-in.
     */
    fun signInWithGoogle() {
        viewModelScope.coroutineScope.launch {
            if (isLabsMode.value) {
                signInToLabsUseCase()
            } else {
                authRepository.signInWithGoogle()
            }
        }
    }

    /**
     * Clears the current error on the active backend, returning to unauthenticated.
     * Call this when user dismisses an error dialog.
     */
    fun dismissError() {
        if (isLabsMode.value) {
            viewModelScope.coroutineScope.launch {
                labsAuthRepository.clearError()
            }
        } else {
            authRepository.clearError()
        }
    }
}
