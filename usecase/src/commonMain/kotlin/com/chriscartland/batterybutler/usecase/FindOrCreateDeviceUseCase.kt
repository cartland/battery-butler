package com.chriscartland.batterybutler.usecase

import com.benasher44.uuid.uuid4
import com.chriscartland.batterybutler.domain.model.DataError
import com.chriscartland.batterybutler.domain.model.Device
import com.chriscartland.batterybutler.domain.model.Result
import com.chriscartland.batterybutler.domain.model.flatMap
import com.chriscartland.batterybutler.domain.model.map
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
    ): Result<Device, DataError> {
        val existingDevices = deviceRepository.getAllDevices().first()
        val existing = existingDevices.find { it.name == deviceName }
        if (existing != null) return Result.Success(existing)

        return findOrCreateDeviceTypeUseCase(deviceTypeName).flatMap { typeId ->
            val device = Device(
                id = uuid4().toString(),
                name = deviceName,
                typeId = typeId,
                batteryLastReplaced = Instant.fromEpochMilliseconds(0),
                lastUpdated = Clock.System.now(),
            )
            deviceRepository.addDevice(device).map { device }
        }
    }
}
