package com.chriscartland.batterybutler.domain.repository

import com.chriscartland.batterybutler.domain.model.BatteryEvent
import com.chriscartland.batterybutler.domain.model.Device
import com.chriscartland.batterybutler.domain.model.DeviceType
import com.chriscartland.batterybutler.domain.model.SyncStatus
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.StateFlow

interface DeviceRepository {
    val syncStatus: StateFlow<SyncStatus>

    fun getAllDevices(): Flow<List<Device>>

    fun getDeviceById(id: String): Flow<Device?>

    suspend fun addDevice(device: Device)

    suspend fun updateDevice(device: Device)

    suspend fun deleteDevice(id: String)

    fun getAllDeviceTypes(): Flow<List<DeviceType>>

    fun getDeviceTypeById(id: String): Flow<DeviceType?>

    suspend fun addDeviceType(type: DeviceType)

    suspend fun updateDeviceType(type: DeviceType)

    suspend fun deleteDeviceType(id: String)

    fun getEventsForDevice(deviceId: String): Flow<List<BatteryEvent>>

    fun getAllEvents(): Flow<List<BatteryEvent>>

    fun getEventById(id: String): Flow<BatteryEvent?>

    suspend fun addEvent(event: BatteryEvent)

    suspend fun updateEvent(event: BatteryEvent)

    suspend fun deleteEvent(id: String)
}
