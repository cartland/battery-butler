package com.chriscartland.batterybutler.usecase

import com.chriscartland.batterybutler.domain.repository.DeviceRepository
import me.tatarka.inject.annotations.Inject

@Inject
class EnsureDefaultDeviceTypesUseCase(
    private val deviceRepository: DeviceRepository,
) {
    suspend operator fun invoke() = deviceRepository.ensureDefaultDeviceTypes()
}
