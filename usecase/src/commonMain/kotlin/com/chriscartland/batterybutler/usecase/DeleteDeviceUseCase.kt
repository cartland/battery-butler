package com.chriscartland.batterybutler.usecase

import com.chriscartland.batterybutler.domain.model.DataError
import com.chriscartland.batterybutler.domain.model.Result
import com.chriscartland.batterybutler.domain.repository.DeviceRepository
import me.tatarka.inject.annotations.Inject

@Inject
class DeleteDeviceUseCase(
    private val deviceRepository: DeviceRepository,
) {
    suspend operator fun invoke(deviceId: String): Result<Unit, DataError> = deviceRepository.deleteDevice(deviceId)
}
