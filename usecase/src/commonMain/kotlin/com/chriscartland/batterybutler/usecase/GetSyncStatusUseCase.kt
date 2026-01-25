package com.chriscartland.batterybutler.usecase

import com.chriscartland.batterybutler.domain.model.SyncStatus
import com.chriscartland.batterybutler.domain.repository.DeviceRepository
import kotlinx.coroutines.flow.StateFlow
import me.tatarka.inject.annotations.Inject

@Inject
class GetSyncStatusUseCase(
    private val repository: DeviceRepository,
) {
    operator fun invoke(): StateFlow<SyncStatus> = repository.syncStatus
}
