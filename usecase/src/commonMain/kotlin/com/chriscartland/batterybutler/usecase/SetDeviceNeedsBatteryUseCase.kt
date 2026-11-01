package com.chriscartland.batterybutler.usecase

import com.chriscartland.batterybutler.domain.model.DataError
import com.chriscartland.batterybutler.domain.model.Result
import com.chriscartland.batterybutler.domain.repository.NeedsBatteryRepository
import me.tatarka.inject.annotations.Inject

/** Marks a device as needing a new battery, or clears that mark. */
@Inject
class SetDeviceNeedsBatteryUseCase(
    private val needsBatteryRepository: NeedsBatteryRepository,
) {
    suspend operator fun invoke(
        deviceId: String,
        needsBattery: Boolean,
    ): Result<Unit, DataError> = needsBatteryRepository.setNeedsBattery(deviceId, needsBattery)
}
