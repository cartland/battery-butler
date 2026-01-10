package com.chriscartland.batterybutler.server.domain.repository

import com.chriscartland.batterybutler.domain.model.BatteryEvent
import com.chriscartland.batterybutler.domain.model.Device
import com.chriscartland.batterybutler.domain.model.DeviceType
import com.chriscartland.batterybutler.domain.repository.RemoteUpdate
import kotlinx.coroutines.flow.Flow

interface ServerDeviceRepository {
    fun getAllDevices(): Flow<List<Device>>

    fun getAllDeviceTypes(): Flow<List<DeviceType>>

    fun getAllEvents(): Flow<List<BatteryEvent>>

    suspend fun addDevice(device: Device)

    suspend fun updateDevice(device: Device)

    suspend fun deleteDevice(id: String)

    suspend fun addDeviceType(type: DeviceType)

    suspend fun updateDeviceType(type: DeviceType)

    suspend fun deleteDeviceType(id: String)

    suspend fun addEvent(event: BatteryEvent)

    suspend fun deleteEvent(id: String)

    // For sync
    fun getUpdates(): Flow<RemoteUpdate>
}
