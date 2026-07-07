package com.chriscartland.batterybutler.usecase

import com.chriscartland.batterybutler.domain.repository.DeviceRepository
import com.chriscartland.batterybutler.domain.repository.LabsAuthRepository
import me.tatarka.inject.annotations.Inject

/**
 * Signs out of the Labs backend and clears the locally cached data for the currently-selected
 * Labs environment, so a signed-out user never sees a previous session's synced devices.
 *
 * Composed here (rather than inside [LabsAuthRepository]'s implementation) so both halves stay
 * independently testable: [LabsAuthRepository]'s real implementation depends on an unfakeable
 * platform `expect class` for Google Sign-In, but this use case only depends on plain interfaces.
 */
@Inject
class SignOutLabsUseCase(
    private val labsAuthRepository: LabsAuthRepository,
    private val deviceRepository: DeviceRepository,
) {
    suspend operator fun invoke() {
        labsAuthRepository.signOutLabs()
        deviceRepository.clearAllLocalData()
    }
}
