package com.chriscartland.batterybutler.usecase

import com.chriscartland.batterybutler.domain.model.DeviceType
import com.chriscartland.batterybutler.domain.repository.DeviceRepository
import kotlinx.coroutines.flow.Flow
import me.tatarka.inject.annotations.Inject

@Inject
class GetDeviceTypesUseCase(
    private val deviceRepository: DeviceRepository,
) {
    operator fun invoke(): Flow<List<DeviceType>> = deviceRepository.getAllDeviceTypes()
}
