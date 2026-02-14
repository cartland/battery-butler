package com.chriscartland.batterybutler.data.repository

import co.touchlab.kermit.Logger
import com.chriscartland.batterybutler.datalocal.LocalDataSource
import com.chriscartland.batterybutler.datanetwork.RemoteDataSource
import com.chriscartland.batterybutler.domain.model.BatteryEvent
import com.chriscartland.batterybutler.domain.model.DataError
import com.chriscartland.batterybutler.domain.model.Device
import com.chriscartland.batterybutler.domain.model.DeviceType
import com.chriscartland.batterybutler.domain.model.SyncStatus
import com.chriscartland.batterybutler.domain.repository.DeviceRepository
import com.chriscartland.batterybutler.domain.repository.RemoteUpdate
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch
import me.tatarka.inject.annotations.Inject
import kotlin.coroutines.cancellation.CancellationException
import kotlin.time.Duration.Companion.milliseconds
import kotlin.time.Duration.Companion.seconds

@Inject
class DefaultDeviceRepository(
    private val localDataSource: LocalDataSource,
    private val remoteDataSource: RemoteDataSource,
    private val scope: CoroutineScope,
) : DeviceRepository {
    private val _syncStatus = MutableStateFlow<SyncStatus>(SyncStatus.Idle)
    override val syncStatus: StateFlow<SyncStatus> = _syncStatus.asStateFlow()

    init {
        scope.launch {
            subscribeWithRetry()
        }
    }

    /**
     * Subscribes to remote updates with exponential backoff on failure.
     *
     * On each successful connection, resets the backoff delay.
     * On failure, waits with increasing delay: 1s -> 2s -> 4s -> 8s -> ... -> 30s max.
     */
    private suspend fun subscribeWithRetry() {
        var backoffMs = INITIAL_BACKOFF_MS
        while (true) {
            try {
                _syncStatus.value = SyncStatus.Syncing
                remoteDataSource.subscribe().collect { update ->
                    // Reset backoff on successful data receipt
                    backoffMs = INITIAL_BACKOFF_MS
                    _syncStatus.value = SyncStatus.Success

                    applyRemoteUpdate(update)
                }
                // subscribe() completed normally (server closed stream) — reconnect
                Logger.d(TAG) { "Subscribe stream ended, reconnecting..." }
            } catch (e: Exception) {
                if (e is CancellationException) throw e
                Logger.e(TAG, e) { "Subscribe failed, retrying in ${backoffMs}ms" }
                _syncStatus.value = SyncStatus.Failed(
                    DataError.Network.ConnectionFailed(
                        message = "Sync disconnected",
                        cause = e.message,
                    ),
                )
            }
            delay(backoffMs.milliseconds)
            backoffMs = (backoffMs * 2).coerceAtMost(MAX_BACKOFF_MS)
        }
    }

    private suspend fun applyRemoteUpdate(update: RemoteUpdate) {
        Logger.d(TAG) { "Received update: ${update.devices.size} devices" }

        if (update.isFullSnapshot) {
            // Design decision: We upsert rather than clear the local DB.
            // This prevents data loss if the network is flaky and avoids
            // orphaning local-only data. Stale data becomes harmless since
            // the server is authoritative. Revisit when implementing
            // offline-first with conflict resolution.
        } else {
            // Apply deletions from server (incremental sync only)
            update.deletedDeviceTypeIds.forEach { localDataSource.deleteDeviceType(it) }
            update.deletedDeviceIds.forEach { localDataSource.deleteDevice(it) }
            update.deletedEventIds.forEach { localDataSource.deleteEvent(it) }
        }
        // Use batch operations for better performance
        localDataSource.addDeviceTypes(update.deviceTypes)
        localDataSource.addDevices(update.devices)
        localDataSource.addEvents(update.events)
    }

    override fun getAllDevices(): Flow<List<Device>> = localDataSource.getAllDevices()

    override fun getDeviceById(id: String): Flow<Device?> = localDataSource.getDeviceById(id)

    override suspend fun addDevice(device: Device) {
        localDataSource.addDevice(device)
        pushUpdate(devices = listOf(device))
    }

    override suspend fun updateDevice(device: Device) {
        localDataSource.updateDevice(device)
        pushUpdate(devices = listOf(device))
    }

    override suspend fun deleteDevice(id: String) {
        localDataSource.deleteDevice(id)
        pushUpdate(deletedDeviceIds = listOf(id))
    }

    override fun getAllDeviceTypes(): Flow<List<DeviceType>> = localDataSource.getAllDeviceTypes()

    override fun getDeviceTypeById(id: String): Flow<DeviceType?> = localDataSource.getDeviceTypeById(id)

    override suspend fun addDeviceType(type: DeviceType) {
        localDataSource.addDeviceType(type)
        pushUpdate(deviceTypes = listOf(type))
    }

    override suspend fun updateDeviceType(type: DeviceType) {
        localDataSource.updateDeviceType(type)
        pushUpdate(deviceTypes = listOf(type))
    }

    override suspend fun deleteDeviceType(id: String) {
        localDataSource.deleteDeviceType(id)
        pushUpdate(deletedDeviceTypeIds = listOf(id))
    }

    override fun getEventsForDevice(deviceId: String): Flow<List<BatteryEvent>> = localDataSource.getEventsForDevice(deviceId)

    override fun getAllEvents(): Flow<List<BatteryEvent>> = localDataSource.getAllEvents()

    override fun getEventById(id: String): Flow<BatteryEvent?> = localDataSource.getEventById(id)

    override suspend fun addEvent(event: BatteryEvent) {
        localDataSource.addEvent(event)
        pushUpdate(events = listOf(event))
    }

    override suspend fun updateEvent(event: BatteryEvent) {
        localDataSource.updateEvent(event)
        pushUpdate(events = listOf(event))
    }

    override suspend fun deleteEvent(id: String) {
        localDataSource.deleteEvent(id)
        pushUpdate(deletedEventIds = listOf(id))
    }

    private fun pushUpdate(
        deviceTypes: List<DeviceType> = emptyList(),
        devices: List<Device> = emptyList(),
        events: List<BatteryEvent> = emptyList(),
        deletedDeviceTypeIds: List<String> = emptyList(),
        deletedDeviceIds: List<String> = emptyList(),
        deletedEventIds: List<String> = emptyList(),
    ) {
        scope.launch {
            _syncStatus.value = SyncStatus.Syncing
            try {
                val success = remoteDataSource.push(
                    RemoteUpdate(
                        isFullSnapshot = false,
                        deviceTypes = deviceTypes,
                        devices = devices,
                        events = events,
                        deletedDeviceTypeIds = deletedDeviceTypeIds,
                        deletedDeviceIds = deletedDeviceIds,
                        deletedEventIds = deletedEventIds,
                    ),
                )
                if (success) {
                    _syncStatus.value = SyncStatus.Success
                    // UI layer is responsible for dismissing Success state after showing feedback
                } else {
                    _syncStatus.value = SyncStatus.Failed(
                        DataError.Network.PushFailed("Server rejected sync request"),
                    )
                }
            } catch (e: Exception) {
                if (e is CancellationException) throw e
                Logger.e("DefaultDeviceRepo", e) { "Push failed" }
                _syncStatus.value = SyncStatus.Failed(
                    DataError.Unknown(e.message ?: "Unknown error", e.toString()),
                )
            }
        }
    }

    override fun dismissSyncStatus() {
        _syncStatus.value = SyncStatus.Idle
    }

    private companion object {
        const val TAG = "DefaultDeviceRepo"
        val INITIAL_BACKOFF_MS = 1.seconds.inWholeMilliseconds
        val MAX_BACKOFF_MS = 30.seconds.inWholeMilliseconds
    }
}
