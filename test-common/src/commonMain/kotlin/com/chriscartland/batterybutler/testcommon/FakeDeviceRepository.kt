package com.chriscartland.batterybutler.testcommon

import com.chriscartland.batterybutler.domain.model.BatteryEvent
import com.chriscartland.batterybutler.domain.model.DataError
import com.chriscartland.batterybutler.domain.model.Device
import com.chriscartland.batterybutler.domain.model.DeviceType
import com.chriscartland.batterybutler.domain.model.Result
import com.chriscartland.batterybutler.domain.model.SyncStatus
import com.chriscartland.batterybutler.domain.repository.DeviceRepository
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.map

/**
 * Fake implementation of [DeviceRepository] for testing.
 *
 * This provides a fully functional in-memory repository that can be used
 * in both unit tests and integration tests. It supports:
 * - Mutable lists for devices, device types, and events
 * - StateFlow-based reactive updates
 * - Configurable sync status
 *
 * Example usage:
 * ```kotlin
 * val repo = FakeDeviceRepository()
 * repo.devices.add(TestDevices.createDevice(id = "1"))
 * repo.setDevices(listOf(device1, device2))
 *
 * // Use in tests
 * val useCase = GetDevicesUseCase(repo)
 * val result = useCase().first()
 * ```
 */
class FakeDeviceRepository : DeviceRepository {
    /** Mutable list of devices. Modify directly or use [setDevices]. */
    val devices = mutableListOf<Device>()

    /** Mutable list of device types. Modify directly or use [setDeviceTypes]. */
    val deviceTypes = mutableListOf<DeviceType>()

    /** Mutable list of battery events. Modify directly or use [setEvents]. */
    val events = mutableListOf<BatteryEvent>()

    private val devicesFlow = MutableStateFlow<List<Device>>(emptyList())
    private val deviceTypesFlow = MutableStateFlow<List<DeviceType>>(emptyList())
    private val eventsFlow = MutableStateFlow<List<BatteryEvent>>(emptyList())
    private val _syncStatus = MutableStateFlow<SyncStatus>(SyncStatus.Idle)

    /** Sets the devices and updates the flow. */
    fun setDevices(newDevices: List<Device>) {
        devices.clear()
        devices.addAll(newDevices)
        devicesFlow.value = devices.toList()
    }

    /** Sets the device types and updates the flow. */
    fun setDeviceTypes(types: List<DeviceType>) {
        deviceTypes.clear()
        deviceTypes.addAll(types)
        deviceTypesFlow.value = deviceTypes.toList()
    }

    /** Sets the events and updates the flow. */
    fun setEvents(newEvents: List<BatteryEvent>) {
        events.clear()
        events.addAll(newEvents)
        eventsFlow.value = events.toList()
    }

    /** Sets the sync status. */
    fun setSyncStatus(status: SyncStatus) {
        _syncStatus.value = status
    }

    // region DeviceRepository implementation

    override val syncStatus: StateFlow<SyncStatus> = _syncStatus

    override fun dismissSyncStatus() {
        _syncStatus.value = SyncStatus.Idle
    }

    override fun getAllDevices(): Flow<List<Device>> = devicesFlow

    override fun getDeviceById(id: String): Flow<Device?> = devicesFlow.map { list -> list.find { it.id == id } }

    override suspend fun addDevice(device: Device): Result<Unit, DataError> {
        devices.add(device)
        devicesFlow.value = devices.toList()
        return Result.Success(Unit)
    }

    override suspend fun updateDevice(device: Device): Result<Unit, DataError> {
        val index = devices.indexOfFirst { it.id == device.id }
        if (index >= 0) {
            devices[index] = device
            devicesFlow.value = devices.toList()
        }
        return Result.Success(Unit)
    }

    override suspend fun deleteDevice(id: String): Result<Unit, DataError> {
        devices.removeAll { it.id == id }
        devicesFlow.value = devices.toList()
        return Result.Success(Unit)
    }

    override fun getAllDeviceTypes(): Flow<List<DeviceType>> = deviceTypesFlow

    override fun getDeviceTypeById(id: String): Flow<DeviceType?> = deviceTypesFlow.map { list -> list.find { it.id == id } }

    override suspend fun addDeviceType(type: DeviceType): Result<Unit, DataError> {
        deviceTypes.add(type)
        deviceTypesFlow.value = deviceTypes.toList()
        return Result.Success(Unit)
    }

    override suspend fun updateDeviceType(type: DeviceType): Result<Unit, DataError> {
        val index = deviceTypes.indexOfFirst { it.id == type.id }
        if (index >= 0) {
            deviceTypes[index] = type
            deviceTypesFlow.value = deviceTypes.toList()
        }
        return Result.Success(Unit)
    }

    override suspend fun deleteDeviceType(id: String): Result<Unit, DataError> {
        deviceTypes.removeAll { it.id == id }
        deviceTypesFlow.value = deviceTypes.toList()
        return Result.Success(Unit)
    }

    override fun getEventsForDevice(deviceId: String): Flow<List<BatteryEvent>> = eventsFlow.map { list -> list.filter { it.deviceId == deviceId } }

    override fun getAllEvents(): Flow<List<BatteryEvent>> = eventsFlow

    override fun getEventById(id: String): Flow<BatteryEvent?> = eventsFlow.map { list -> list.find { it.id == id } }

    override suspend fun addEvent(event: BatteryEvent): Result<Unit, DataError> {
        events.add(event)
        eventsFlow.value = events.toList()
        return Result.Success(Unit)
    }

    override suspend fun updateEvent(event: BatteryEvent): Result<Unit, DataError> {
        val index = events.indexOfFirst { it.id == event.id }
        if (index >= 0) {
            events[index] = event
            eventsFlow.value = events.toList()
        }
        return Result.Success(Unit)
    }

    override suspend fun deleteEvent(id: String): Result<Unit, DataError> {
        events.removeAll { it.id == id }
        eventsFlow.value = events.toList()
        return Result.Success(Unit)
    }

    // endregion
}
