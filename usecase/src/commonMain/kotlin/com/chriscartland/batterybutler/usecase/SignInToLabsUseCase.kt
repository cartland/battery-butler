package com.chriscartland.batterybutler.usecase

import com.chriscartland.batterybutler.domain.model.AuthError
import com.chriscartland.batterybutler.domain.model.Result
import com.chriscartland.batterybutler.domain.model.User
import com.chriscartland.batterybutler.domain.repository.DeviceRepository
import com.chriscartland.batterybutler.domain.repository.LabsAuthRepository
import me.tatarka.inject.annotations.Inject
import kotlin.time.Duration.Companion.seconds

/**
 * Signs in to the Labs backend and, on success, triggers an immediate resync so the currently
 * selected Labs environment's devices populate right away instead of waiting for the ambient
 * background sync loop (whose retry backoff can drift up to 30s while signed out).
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
) {
    suspend operator fun invoke(): Result<User, AuthError> {
        val result = labsAuthRepository.signInToLabs()
        if (result is Result.Success) {
            // resync() now reports a typed SyncOutcome (success / auth-required / failed) instead
            // of masquerading every failure as a silent no-op. The outcome already reaches the user
            // via syncStatus (AuthRequired renders as "sign in required"); *reacting* to it here —
            // e.g. treating a post-sign-in AuthRequired as a failed sign-in — is deliberately left
            // to the auth-state reaction follow-up PR.
            deviceRepository.resync(timeout = SIGN_IN_RESYNC_TIMEOUT)
        }
        return result
    }

    private companion object {
        val SIGN_IN_RESYNC_TIMEOUT = 60.seconds
    }
}
