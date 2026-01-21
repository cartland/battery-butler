package com.chriscartland.batterybutler.server.app

import com.chriscartland.batterybutler.domain.AppLogger
import com.chriscartland.batterybutler.proto.PushResponse
import com.chriscartland.batterybutler.proto.SyncServiceGrpcKt
import com.chriscartland.batterybutler.proto.SyncUpdate
import com.chriscartland.batterybutler.server.domain.repository.ServerDeviceRepository
import com.google.protobuf.Empty
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.map

class SyncService(
    private val repository: ServerDeviceRepository,
) : SyncServiceGrpcKt.SyncServiceCoroutineImplBase() {
    init {
        // Kept for debug_flow.sh verification
        // AppLogger.d("BatteryButlerDebug", "SyncService Created")
    }

    override fun subscribe(request: Empty): Flow<SyncUpdate> {
        AppLogger.d("BatteryButlerServer", "SyncService subscribe called")
        return repository.getUpdates().map {
            // Only log if needed, reducing noise
            // AppLogger.d("BatteryButlerDebug", "SyncService mapping update size=${it.devices.size}")
            ServerSyncMapper.toProto(it)
        }
    }

    override suspend fun pushUpdate(request: SyncUpdate): PushResponse {
        val domainUpdate = ServerSyncMapper.toDomain(request)

        // Apply updates to repository
        domainUpdate.deviceTypes.forEach { repository.addDeviceType(it) } // naive add/update
        domainUpdate.devices.forEach { repository.addDevice(it) }
        domainUpdate.events.forEach { repository.addEvent(it) }

        return PushResponse.newBuilder().setSuccess(true).build()
    }
}
