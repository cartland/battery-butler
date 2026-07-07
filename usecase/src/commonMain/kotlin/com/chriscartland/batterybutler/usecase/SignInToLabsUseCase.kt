package com.chriscartland.batterybutler.usecase

import com.chriscartland.batterybutler.domain.model.AuthError
import com.chriscartland.batterybutler.domain.model.Result
import com.chriscartland.batterybutler.domain.model.User
import com.chriscartland.batterybutler.domain.repository.DeviceRepository
import com.chriscartland.batterybutler.domain.repository.LabsAuthRepository
import me.tatarka.inject.annotations.Inject

/**
 * Signs in to the Labs backend and, on success, triggers an immediate resync so the currently
 * selected Labs environment's devices populate right away instead of waiting for the ambient
 * background sync loop (whose retry backoff can drift up to 30s while signed out).
 */
@Inject
class SignInToLabsUseCase(
    private val labsAuthRepository: LabsAuthRepository,
    private val deviceRepository: DeviceRepository,
) {
    suspend operator fun invoke(): Result<User, AuthError> {
        val result = labsAuthRepository.signInToLabs()
        if (result is Result.Success) {
            deviceRepository.resync()
        }
        return result
    }
}
