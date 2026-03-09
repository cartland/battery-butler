package com.chriscartland.batterybutler.usecase

import com.benasher44.uuid.uuid4
import com.chriscartland.batterybutler.domain.model.DataError
import com.chriscartland.batterybutler.domain.model.DeviceType
import com.chriscartland.batterybutler.domain.model.Result
import com.chriscartland.batterybutler.domain.model.map
import com.chriscartland.batterybutler.domain.repository.DeviceRepository
import kotlinx.coroutines.flow.first
import me.tatarka.inject.annotations.Inject

/**
 * Finds an existing device type by name, or creates a new one with default settings.
 *
 * Returns the ID of the found or created device type.
 * If [typeName] is null or blank, returns "default_type".
 */
@Inject
class FindOrCreateDeviceTypeUseCase(
    private val deviceRepository: DeviceRepository,
) {
    suspend operator fun invoke(typeName: String?): Result<String, DataError> {
        if (typeName.isNullOrBlank()) {
            return Result.Success("default_type")
        }
        val existingTypes = deviceRepository.getAllDeviceTypes().first()
        val existingType = existingTypes.find { it.name == typeName }
        if (existingType != null) {
            return Result.Success(existingType.id)
        }

        val newTypeId = uuid4().toString()
        return deviceRepository
            .addDeviceType(
                DeviceType(id = newTypeId, name = typeName, defaultIcon = "default"),
            ).map { newTypeId }
    }
}
