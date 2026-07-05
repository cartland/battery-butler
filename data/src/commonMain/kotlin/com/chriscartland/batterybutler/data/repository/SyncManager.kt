package com.chriscartland.batterybutler.data.repository

import com.chriscartland.batterybutler.domain.model.BatteryEvent
import com.chriscartland.batterybutler.domain.model.Device
import com.chriscartland.batterybutler.domain.model.DeviceType
import com.chriscartland.batterybutler.domain.model.SyncStatus
import kotlinx.coroutines.flow.StateFlow

/**
 * Manages remote synchronization: subscribes for updates, pushes local changes,
 * and exposes the current [syncStatus].
 *
 * Separated from [DeviceRepository] to keep CRUD and sync as independent concerns.
 */
interface SyncManager {
    val syncStatus: StateFlow<SyncStatus>

    fun dismissSyncStatus()

    /**
     * Performs one immediate remote fetch (bypassing the background retry loop's backoff
     * delay) and applies the result locally. Bounded by a timeout so it always resolves even
     * against a server-streaming source with nothing new to push. Safe to call concurrently
     * with the background sync loop -- applying the same snapshot twice is a harmless no-op.
     */
    suspend fun resync()

    fun pushUpdate(
        deviceTypes: List<DeviceType> = emptyList(),
        devices: List<Device> = emptyList(),
        events: List<BatteryEvent> = emptyList(),
        deletedDeviceTypeIds: List<String> = emptyList(),
        deletedDeviceIds: List<String> = emptyList(),
        deletedEventIds: List<String> = emptyList(),
    )
}
