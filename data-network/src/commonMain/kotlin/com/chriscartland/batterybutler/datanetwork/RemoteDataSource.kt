package com.chriscartland.batterybutler.datanetwork

import com.chriscartland.batterybutler.domain.model.BatteryEvent
import com.chriscartland.batterybutler.domain.model.Device
import com.chriscartland.batterybutler.domain.model.DeviceType
import kotlinx.coroutines.flow.Flow

interface RemoteDataSource {
    fun subscribe(): Flow<RemoteUpdate>

    suspend fun push(update: RemoteUpdate): Boolean
}

data class RemoteUpdate(
    val isFullSnapshot: Boolean,
    val deviceTypes: List<DeviceType>,
    val devices: List<Device>,
    val events: List<BatteryEvent>,
)
