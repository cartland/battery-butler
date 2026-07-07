package com.chriscartland.batterybutler.data.repository

import com.chriscartland.batterybutler.datalocal.LocalDataSource
import com.chriscartland.batterybutler.domain.model.BatteryEvent
import com.chriscartland.batterybutler.domain.model.DataError
import com.chriscartland.batterybutler.domain.model.Device
import com.chriscartland.batterybutler.domain.model.DeviceType
import com.chriscartland.batterybutler.domain.model.Result
import com.chriscartland.batterybutler.domain.model.SyncStatus
import com.chriscartland.batterybutler.domain.repository.DeviceRepository
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.StateFlow
import me.tatarka.inject.annotations.Inject
import kotlin.coroutines.cancellation.CancellationException
import kotlin.time.Duration

@Inject
class DefaultDeviceRepository(
    private val localDataSource: LocalDataSource,
    private val syncManager: SyncManager,
) : DeviceRepository {
    override val syncStatus: StateFlow<SyncStatus> = syncManager.syncStatus

    override fun dismissSyncStatus() = syncManager.dismissSyncStatus()

    override suspend fun resync(timeout: Duration) = syncManager.resync(timeout)

    override suspend fun clearAllLocalData() = localDataSource.clearAll()

    override fun getAllDevices(): Flow<List<Device>> = localDataSource.getAllDevices()

    override fun getDeviceById(id: String): Flow<Device?> = localDataSource.getDeviceById(id)

    override suspend fun addDevice(device: Device): Result<Unit, DataError> =
        runWriteOperation("Failed to add device") {
            localDataSource.addDevice(device)
            syncManager.pushUpdate(devices = listOf(device))
        }

    override suspend fun updateDevice(device: Device): Result<Unit, DataError> =
        runWriteOperation("Failed to update device") {
            localDataSource.updateDevice(device)
            syncManager.pushUpdate(devices = listOf(device))
        }

    override suspend fun deleteDevice(id: String): Result<Unit, DataError> =
        runWriteOperation("Failed to delete device") {
            localDataSource.deleteDevice(id)
            syncManager.pushUpdate(deletedDeviceIds = listOf(id))
        }

    override fun getAllDeviceTypes(): Flow<List<DeviceType>> = localDataSource.getAllDeviceTypes()

    override fun getDeviceTypeById(id: String): Flow<DeviceType?> = localDataSource.getDeviceTypeById(id)

    override suspend fun addDeviceType(type: DeviceType): Result<Unit, DataError> =
        runWriteOperation("Failed to add device type") {
            localDataSource.addDeviceType(type)
            syncManager.pushUpdate(deviceTypes = listOf(type))
        }

    override suspend fun updateDeviceType(type: DeviceType): Result<Unit, DataError> =
        runWriteOperation("Failed to update device type") {
            localDataSource.updateDeviceType(type)
            syncManager.pushUpdate(deviceTypes = listOf(type))
        }

    override suspend fun deleteDeviceType(id: String): Result<Unit, DataError> =
        runWriteOperation("Failed to delete device type") {
            localDataSource.deleteDeviceType(id)
            syncManager.pushUpdate(deletedDeviceTypeIds = listOf(id))
        }

    override fun getEventsForDevice(deviceId: String): Flow<List<BatteryEvent>> = localDataSource.getEventsForDevice(deviceId)

    override fun getAllEvents(): Flow<List<BatteryEvent>> = localDataSource.getAllEvents()

    override fun getEventById(id: String): Flow<BatteryEvent?> = localDataSource.getEventById(id)

    override suspend fun addEvent(event: BatteryEvent): Result<Unit, DataError> =
        runWriteOperation("Failed to add event") {
            localDataSource.addEvent(event)
            syncManager.pushUpdate(events = listOf(event))
        }

    override suspend fun updateEvent(event: BatteryEvent): Result<Unit, DataError> =
        runWriteOperation("Failed to update event") {
            localDataSource.updateEvent(event)
            syncManager.pushUpdate(events = listOf(event))
        }

    override suspend fun deleteEvent(id: String): Result<Unit, DataError> =
        runWriteOperation("Failed to delete event") {
            localDataSource.deleteEvent(id)
            syncManager.pushUpdate(deletedEventIds = listOf(id))
        }

    private inline fun runWriteOperation(
        failureMessage: String,
        block: () -> Unit,
    ): Result<Unit, DataError> =
        try {
            block()
            Result.Success(Unit)
        } catch (e: CancellationException) {
            throw e
        } catch (e: Exception) {
            Result.Error(
                DataError.Database.WriteFailed(
                    message = e.message ?: failureMessage,
                    cause = e.toString(),
                ),
            )
        }
}
