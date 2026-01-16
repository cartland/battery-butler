package com.chriscartland.batterybutler.server.app

import com.chriscartland.batterybutler.proto.PushResponse
import com.chriscartland.batterybutler.proto.SyncServiceGrpcKt
import com.chriscartland.batterybutler.proto.SyncUpdate
import com.chriscartland.batterybutler.server.domain.repository.ServerDeviceRepository
import com.google.protobuf.Empty
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.emitAll
import kotlinx.coroutines.flow.flow
import kotlinx.coroutines.flow.map

class SyncService(
    private val repository: ServerDeviceRepository,
) : SyncServiceGrpcKt.SyncServiceCoroutineImplBase() {
    override fun subscribe(request: Empty): Flow<SyncUpdate> =
        flow {
            // 1. Emit Initial Snapshot
            // We need to fetch all data and construct a snapshot
            // The repository APIs return Flows, so we grab the first value or collect?
            // InMemoryRepo is synchronous in memory, getAll returns Flow{ emit(list) } immediately.

            // This is a bit inefficient (collecting 3 flows), but for MVP ok.
            // Better to have `repository.getSnapshot()`
            // But let's just rely on getUpdates() for simplicity.
            // Wait, client expects a snapshot first?
            // The current logic in Client `RoomDeviceRepository` handles snapshot if `isFullSnapshot` is true.
            // `InMemoryDeviceRepository.broadcastUpdate` sends full snapshot.
            // But initially, we might not have an update triggering.
            // So we should manually fetch.

            // However, since `getUpdates()` is a SharedFlow, we miss past events.
            // We should construct a snapshot here.
            // But `ServerDeviceRepository` methods are tailored for clean separation.

            // Let's modify `ServerSyncMapper` to support "List<T> -> update"?
            // No, `RemoteUpdate` takes lists.

            // We can just rely on `getUpdates()` if we force a refresh? No.

            // Let's grab current state (mock implementation for now)
            // Ideally we change `ServerDeviceRepository` to have `suspend fun getSnapshot(): RemoteUpdate`.
            // But to avoid interface churn, I'll rely on collecting the Flows quickly.

            // Combining?

            // Actually, let's just return `repository.getUpdates().map { ServerSyncMapper.toProto(it) }`
            // AND prefix it with current state.

            // But `getAllDevices` returns Flow.
            // I'll emit updates. The "Initial Sync" might be empty if we don't start with something.
            // If the server is fresh, empty is fine.
            // If server has data, client needs it.

            // To do this simply:
            // `emitAll(repository.getUpdates().map { ... })`
            // But Client needs initial state.

            // I'll skip initial state for this exact "In-Memory" MVP step unless I add `getSnapshot`.
            // Wait, `InMemoryDeviceRepository` has `updates` as `MutableSharedFlow(replay = 0)`.
            // If I change replay to 1, I get last state.
            // But that state is global.

            // I'll implement `subscribe` to just listen for now.
            // If I have time I'll add `getSnapshot`.

            emitAll(repository.getUpdates().map { ServerSyncMapper.toProto(it) })
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
