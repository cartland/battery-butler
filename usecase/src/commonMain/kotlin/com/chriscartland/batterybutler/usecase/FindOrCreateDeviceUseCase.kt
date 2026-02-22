package com.chriscartland.batterybutler.usecase

import com.benasher44.uuid.uuid4
import com.chriscartland.batterybutler.domain.model.Device
import com.chriscartland.batterybutler.domain.repository.DeviceRepository
import kotlinx.coroutines.flow.first
import me.tatarka.inject.annotations.Inject
import kotlin.time.Clock
import kotlin.time.Instant

/**
 * Finds an existing device by name, or creates a new one.
 *
 * When creating, uses [FindOrCreateDeviceTypeUseCase] to resolve the device type.
 */
@Inject
class FindOrCreateDeviceUseCase(
    private val deviceRepository: DeviceRepository,
    private val findOrCreateDeviceTypeUseCase: FindOrCreateDeviceTypeUseCase,
) {
    suspend operator fun invoke(
        deviceName: String,
        deviceTypeName: String? = null,
    ): Device {
        val existingDevices = deviceRepository.getAllDevices().first()
        return existingDevices.find { it.name == deviceName }
            ?: run {
                val typeId = findOrCreateDeviceTypeUseCase(deviceTypeName)
                Device(
                    id = uuid4().toString(),
                    name = deviceName,
                    typeId = typeId,
                    batteryLastReplaced = Instant.fromEpochMilliseconds(0),
                    lastUpdated = Clock.System.now(),
                ).also { deviceRepository.addDevice(it) }
            }
    }
}
