package com.chriscartland.batterybutler.usecase

import com.chriscartland.batterybutler.domain.repository.DeviceRepository
import me.tatarka.inject.annotations.Inject

@Inject
class DismissSyncStatusUseCase(
    private val repository: DeviceRepository,
) {
    operator fun invoke() {
        repository.dismissSyncStatus()
    }
}
