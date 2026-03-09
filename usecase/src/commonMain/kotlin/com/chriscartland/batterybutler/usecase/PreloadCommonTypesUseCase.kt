package com.chriscartland.batterybutler.usecase

import com.benasher44.uuid.uuid4
import com.chriscartland.batterybutler.domain.model.CommonDeviceTypes
import com.chriscartland.batterybutler.domain.model.DataError
import com.chriscartland.batterybutler.domain.model.DeviceType
import com.chriscartland.batterybutler.domain.model.Result
import com.chriscartland.batterybutler.domain.repository.DeviceRepository
import kotlinx.coroutines.flow.first
import me.tatarka.inject.annotations.Inject

@Inject
class PreloadCommonTypesUseCase(
    private val deviceRepository: DeviceRepository,
) {
    suspend operator fun invoke(): Result<Unit, DataError> {
        val existingTypes = deviceRepository.getAllDeviceTypes().first()
        val existingNames = existingTypes.map { it.name }.toSet()

        for (template in CommonDeviceTypes.types) {
            if (template.name in existingNames) continue
            val result = deviceRepository.addDeviceType(
                DeviceType(
                    id = uuid4().toString(),
                    name = template.name,
                    batteryType = template.batteryType,
                    batteryQuantity = template.batteryQuantity,
                    defaultIcon = template.defaultIcon,
                ),
            )
            if (result is Result.Error) return result
        }
        return Result.Success(Unit)
    }
}
