package com.chriscartland.batterybutler.usecase

import com.chriscartland.batterybutler.domain.model.Device
import com.chriscartland.batterybutler.domain.repository.DeviceRepository
import kotlinx.coroutines.flow.Flow
import me.tatarka.inject.annotations.Inject

@Inject
class GetDevicesUseCase(
    private val deviceRepository: DeviceRepository,
) {
    operator fun invoke(): Flow<List<Device>> = deviceRepository.getAllDevices()
}
