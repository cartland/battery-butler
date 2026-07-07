package com.chriscartland.batterybutler.usecase

import com.chriscartland.batterybutler.domain.repository.AuthRepository
import com.chriscartland.batterybutler.domain.repository.DeviceRepository
import me.tatarka.inject.annotations.Inject

/**
 * Signs out of the app's own gRPC account and clears the locally cached data for the
 * currently-selected gRPC environment, so a signed-out user never sees a previous session's
 * synced devices.
 *
 * Composed here (rather than inside [AuthRepository]'s implementation) so both halves stay
 * independently testable, mirroring [SignOutLabsUseCase] for the Labs backend.
 */
@Inject
class SignOutUseCase(
    private val authRepository: AuthRepository,
    private val deviceRepository: DeviceRepository,
) {
    suspend operator fun invoke() {
        authRepository.signOut()
        deviceRepository.clearAllLocalData()
    }
}
