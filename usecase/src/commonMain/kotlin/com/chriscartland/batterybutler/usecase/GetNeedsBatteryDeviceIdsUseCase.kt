package com.chriscartland.batterybutler.usecase

import com.chriscartland.batterybutler.domain.repository.NeedsBatteryRepository
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.map
import me.tatarka.inject.annotations.Inject

/**
 * Observes which devices are currently marked as needing a new battery.
 *
 * Membership only. Use [GetNeedsBatteryMarksUseCase] when the caller also needs to know *when*
 * each mark was made.
 */
@Inject
class GetNeedsBatteryDeviceIdsUseCase(
    private val needsBatteryRepository: NeedsBatteryRepository,
) {
    operator fun invoke(): Flow<Set<String>> = needsBatteryRepository.getNeedsBatteryMarks().map { it.keys }
}
