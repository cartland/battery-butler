package com.chriscartland.batterybutler.usecase

import com.chriscartland.batterybutler.domain.repository.NeedsBatteryRepository
import kotlinx.coroutines.flow.Flow
import me.tatarka.inject.annotations.Inject
import kotlin.time.Instant

/**
 * Observes every "needs a new battery" mark with the moment it was made.
 *
 * The device list's `RECENT` sort ranks a device by its most recent activity, and marking one is
 * activity. [GetNeedsBatteryDeviceIdsUseCase] is the membership-only view over the same data.
 */
@Inject
class GetNeedsBatteryMarksUseCase(
    private val needsBatteryRepository: NeedsBatteryRepository,
) {
    operator fun invoke(): Flow<Map<String, Instant>> = needsBatteryRepository.getNeedsBatteryMarks()
}
