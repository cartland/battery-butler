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
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch
import me.tatarka.inject.annotations.Inject
import kotlin.coroutines.cancellation.CancellationException

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
            try {
                remoteDataSource.subscribe().collect { update ->
                    Logger.d("BatteryButlerRepo") { "DefaultDeviceRepository received update! Size=${update.devices.size}" }

                    if (update.isFullSnapshot) {
                        // Design decision: We upsert rather than clear the local DB.
                        // This prevents data loss if the network is flaky and avoids
                        // orphaning local-only data. Stale data becomes harmless since
                        // the server is authoritative. Revisit when implementing
                        // offline-first with conflict resolution.
                    }
                    // Use batch operations for better performance
                    localDataSource.addDeviceTypes(update.deviceTypes)
                    localDataSource.addDevices(update.devices)
                    localDataSource.addEvents(update.events)
                }
            } catch (e: Exception) {
                if (e is CancellationException) throw e
                Logger.e("BatteryButlerRepo", e) { "Error receiving remote updates" }
            }
        }
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
        // Note: Deletes are local-only. RemoteUpdate only supports add/update semantics.
        // Remote delete requires either: (1) adding deletedIds to RemoteUpdate proto,
        // or (2) soft-delete with isDeleted flag. See issue tracker for remote delete support.
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
    }

    private fun pushUpdate(
        deviceTypes: List<DeviceType> = emptyList(),
        devices: List<Device> = emptyList(),
        events: List<BatteryEvent> = emptyList(),
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
}
