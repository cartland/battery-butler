package com.chriscartland.batterybutler.usecase

import com.chriscartland.batterybutler.domain.repository.NeedsBatteryRepository
import kotlinx.coroutines.flow.Flow
import me.tatarka.inject.annotations.Inject

/** Observes which devices are currently marked as needing a new battery. */
@Inject
class GetNeedsBatteryDeviceIdsUseCase(
    private val needsBatteryRepository: NeedsBatteryRepository,
) {
    operator fun invoke(): Flow<Set<String>> = needsBatteryRepository.getFlaggedDeviceIds()
}
