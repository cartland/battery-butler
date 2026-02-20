package com.chriscartland.batterybutler.usecase

import com.benasher44.uuid.uuid4
import com.chriscartland.batterybutler.domain.model.CommonDeviceTypes
import com.chriscartland.batterybutler.domain.model.DeviceType
import com.chriscartland.batterybutler.domain.repository.DeviceRepository
import kotlinx.coroutines.flow.first
import me.tatarka.inject.annotations.Inject

@Inject
class PreloadCommonTypesUseCase(
    private val deviceRepository: DeviceRepository,
) {
    suspend operator fun invoke() {
        val existingTypes = deviceRepository.getAllDeviceTypes().first()
        val existingNames = existingTypes.map { it.name }.toSet()

        for (template in CommonDeviceTypes.types) {
            if (template.name in existingNames) continue
            deviceRepository.addDeviceType(
                DeviceType(
                    id = uuid4().toString(),
                    name = template.name,
                    batteryType = template.batteryType,
                    batteryQuantity = template.batteryQuantity,
                    defaultIcon = template.defaultIcon,
                ),
            )
        }
    }
}
