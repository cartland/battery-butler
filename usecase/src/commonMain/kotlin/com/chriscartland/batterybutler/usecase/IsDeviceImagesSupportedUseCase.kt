package com.chriscartland.batterybutler.usecase

import com.chriscartland.batterybutler.domain.repository.DeviceImageRepository
import kotlinx.coroutines.flow.Flow
import me.tatarka.inject.annotations.Inject

/** True only when the currently-selected backend supports device images (Labs modes). */
@Inject
class IsDeviceImagesSupportedUseCase(
    private val deviceImageRepository: DeviceImageRepository,
) {
    operator fun invoke(): Flow<Boolean> = deviceImageRepository.supported
}
