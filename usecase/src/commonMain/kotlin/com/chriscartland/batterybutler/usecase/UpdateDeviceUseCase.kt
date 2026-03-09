package com.chriscartland.batterybutler.usecase

import com.chriscartland.batterybutler.domain.model.DataError
import com.chriscartland.batterybutler.domain.model.Device
import com.chriscartland.batterybutler.domain.model.Result
import com.chriscartland.batterybutler.domain.repository.DeviceRepository
import me.tatarka.inject.annotations.Inject

@Inject
class UpdateDeviceUseCase(
    private val deviceRepository: DeviceRepository,
) {
    suspend operator fun invoke(device: Device): Result<Unit, DataError> = deviceRepository.updateDevice(device)
}
