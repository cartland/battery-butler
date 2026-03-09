package com.chriscartland.batterybutler.usecase

import com.chriscartland.batterybutler.domain.model.DataError
import com.chriscartland.batterybutler.domain.model.DeviceType
import com.chriscartland.batterybutler.domain.model.Result
import com.chriscartland.batterybutler.domain.repository.DeviceRepository
import me.tatarka.inject.annotations.Inject

@Inject
class AddDeviceTypeUseCase(
    private val deviceRepository: DeviceRepository,
) {
    suspend operator fun invoke(deviceType: DeviceType): Result<Unit, DataError> = deviceRepository.addDeviceType(deviceType)
}
