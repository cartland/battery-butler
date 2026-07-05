package com.chriscartland.batterybutler.usecase

import com.chriscartland.batterybutler.domain.repository.DeviceRepository
import me.tatarka.inject.annotations.Inject

// @NoTestRequired: Thin wrapper, single-line delegation to repository
@Inject
class ResyncUseCase(
    private val repository: DeviceRepository,
) {
    suspend operator fun invoke() = repository.resync()
}
