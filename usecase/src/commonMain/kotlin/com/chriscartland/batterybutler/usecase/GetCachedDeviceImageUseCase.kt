package com.chriscartland.batterybutler.usecase

import com.chriscartland.batterybutler.domain.model.DeviceImageBytes
import com.chriscartland.batterybutler.domain.repository.DeviceImageRepository
import kotlinx.coroutines.flow.Flow
import me.tatarka.inject.annotations.Inject

@Inject
class GetCachedDeviceImageUseCase(
    private val deviceImageRepository: DeviceImageRepository,
) {
    operator fun invoke(imageEtag: String): Flow<DeviceImageBytes?> = deviceImageRepository.observeCachedImage(imageEtag)
}
