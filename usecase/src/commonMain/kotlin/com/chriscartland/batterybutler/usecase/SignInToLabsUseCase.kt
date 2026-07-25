package com.chriscartland.batterybutler.usecase

import com.chriscartland.batterybutler.domain.model.AuthError
import com.chriscartland.batterybutler.domain.model.Result
import com.chriscartland.batterybutler.domain.model.User
import com.chriscartland.batterybutler.domain.repository.DeviceRepository
import com.chriscartland.batterybutler.domain.repository.LabsAuthRepository
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.launch
import me.tatarka.inject.annotations.Inject
import kotlin.time.Duration.Companion.seconds

/**
 * Signs in to the Labs backend and, on success, triggers an immediate resync so the currently
 * selected Labs environment's devices populate right away instead of waiting for the ambient
 * background sync loop (whose retry backoff can drift up to 30s while signed out).
 *
 * The resync runs on the injected **app-lifetime [scope]**, not the caller's: sign-in flips the
 * auth state, the UI navigates, and a viewModelScope-launched resync would be torn down
 * mid-flight by that navigation (the documented residual of `bb-signin-empty-list` in TODO.md) —
 * so the sync either silently died or never applied. Launched on the app scope it always runs to
 * completion; its outcome reaches the user through `syncStatus` (AuthRequired renders as "sign
 * in required", failures as sync errors), which is why the [SyncOutcome] is not returned here —
 * surfacing it would change this signature for a value the status stream already carries.
 *
 * Uses a longer-than-default [SIGN_IN_RESYNC_TIMEOUT]: the user is already in a natural loading
 * moment after tapping sign-in (unlike pull-to-refresh's interactive spinner), and a Labs backend
 * that's been idle for a while can take longer than the default 15s to respond to its first
 * request. See `bb-labs-cold-resync` in TODO.md.
 */
@Inject
class SignInToLabsUseCase(
    private val labsAuthRepository: LabsAuthRepository,
    private val deviceRepository: DeviceRepository,
    private val scope: CoroutineScope,
) {
    suspend operator fun invoke(): Result<User, AuthError> {
        val result = labsAuthRepository.signInToLabs()
        if (result is Result.Success) {
            scope.launch {
                deviceRepository.resync(timeout = SIGN_IN_RESYNC_TIMEOUT)
            }
        }
        return result
    }

    private companion object {
        val SIGN_IN_RESYNC_TIMEOUT = 60.seconds
    }
}
