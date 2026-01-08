package com.chriscartland.batterybutler.usecase

import com.chriscartland.batterybutler.domain.model.Device
import com.chriscartland.batterybutler.domain.repository.DeviceRepository
import kotlinx.coroutines.flow.Flow
import me.tatarka.inject.annotations.Inject

@Inject
class GetDeviceDetailUseCase(
    private val deviceRepository: DeviceRepository,
) {
    operator fun invoke(deviceId: String): Flow<Device?> = deviceRepository.getDeviceById(deviceId)
}
