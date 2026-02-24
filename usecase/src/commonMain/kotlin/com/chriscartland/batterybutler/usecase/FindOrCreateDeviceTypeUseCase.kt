package com.chriscartland.batterybutler.usecase

import com.benasher44.uuid.uuid4
import com.chriscartland.batterybutler.domain.model.DeviceType
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
    suspend operator fun invoke(typeName: String?): String {
        if (typeName.isNullOrBlank()) {
            return "default_type"
        }
        val existingTypes = deviceRepository.getAllDeviceTypes().first()
        val existingType = existingTypes.find { it.name == typeName }
        if (existingType != null) {
            return existingType.id
        }

        val newTypeId = uuid4().toString()
        deviceRepository.addDeviceType(
            DeviceType(id = newTypeId, name = typeName, defaultIcon = "default"),
        )
        return newTypeId
    }
}
