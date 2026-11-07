package com.chriscartland.batterybutler.datalocal

import com.chriscartland.batterybutler.domain.model.BatteryEvent
import com.chriscartland.batterybutler.domain.model.Device
import com.chriscartland.batterybutler.domain.model.DeviceType
import kotlinx.coroutines.flow.Flow

/**
 * The ids a full snapshot contained, per table.
 *
 * This is what makes "missing from the snapshot" mean "deleted on the server" rather than "never
 * reached the server". Pushes are fire-and-forget with no retry, so a row logged offline is absent
 * from every later snapshot. A row can only be a server-side deletion if the server had it before:
 * it was in the previous snapshot and isn't in this one.
 */
data class SnapshotIds(
    val deviceTypeIds: Set<String> = emptySet(),
    val deviceIds: Set<String> = emptySet(),
    val eventIds: Set<String> = emptySet(),
)

interface LocalDataSource {
    fun getAllDevices(): Flow<List<Device>>

    fun getDeviceById(id: String): Flow<Device?>

    suspend fun addDevice(device: Device)

    suspend fun addDevices(devices: List<Device>)

    suspend fun updateDevice(device: Device)

    suspend fun deleteDevice(id: String)

    fun getAllDeviceTypes(): Flow<List<DeviceType>>

    fun getDeviceTypeById(id: String): Flow<DeviceType?>

    suspend fun addDeviceType(type: DeviceType)

    suspend fun addDeviceTypes(types: List<DeviceType>)

    suspend fun updateDeviceType(type: DeviceType)

    suspend fun deleteDeviceType(id: String)

    fun getEventsForDevice(deviceId: String): Flow<List<BatteryEvent>>

    fun getAllEvents(): Flow<List<BatteryEvent>>

    fun getEventById(id: String): Flow<BatteryEvent?>

    suspend fun addEvent(event: BatteryEvent)

    suspend fun addEvents(events: List<BatteryEvent>)

    suspend fun updateEvent(event: BatteryEvent)

    suspend fun deleteEvent(id: String)

    /** The ids of the last full snapshot applied to the active store; empty if there was none. */
    suspend fun getLastSnapshotIds(): SnapshotIds

    suspend fun setLastSnapshotIds(ids: SnapshotIds)

    /**
     * Deletes all devices, device types, events and the last snapshot's ids from the currently
     * active local store.
     */
    suspend fun clearAll()
}
