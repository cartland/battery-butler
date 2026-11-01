package com.chriscartland.batterybutler.usecase

import com.chriscartland.batterybutler.domain.model.DataError
import com.chriscartland.batterybutler.domain.model.Result
import com.chriscartland.batterybutler.domain.model.flatMap
import com.chriscartland.batterybutler.domain.repository.DeviceRepository
import me.tatarka.inject.annotations.Inject

@Inject
class DeleteDeviceUseCase(
    private val deviceRepository: DeviceRepository,
    private val setDeviceNeedsBattery: SetDeviceNeedsBatteryUseCase,
) {
    suspend operator fun invoke(deviceId: String): Result<Unit, DataError> =
        deviceRepository.deleteDevice(deviceId).flatMap {
            // The mark lives in its own table with no foreign key to `devices`, so nothing drops it
            // for us. Left behind, it would silently re-attach if the same device id ever came back
            // from a sync.
            setDeviceNeedsBattery(deviceId, needsBattery = false)
        }
}
