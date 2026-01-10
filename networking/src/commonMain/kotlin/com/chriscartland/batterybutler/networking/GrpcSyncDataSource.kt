package com.chriscartland.batterybutler.networking

import com.chriscartland.batterybutler.domain.repository.RemoteDataSource
import com.chriscartland.batterybutler.domain.repository.RemoteUpdate
import com.chriscartland.batterybutler.proto.SyncServiceClient
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.channelFlow
import kotlinx.coroutines.flow.consumeAsFlow
import kotlinx.coroutines.flow.map

class GrpcSyncDataSource(
    private val client: SyncServiceClient,
) : RemoteDataSource {
    override fun subscribe(): Flow<RemoteUpdate> =
        channelFlow {
            // executeIn returns (SendChannel, ReceiveChannel)
            val (_, responseChannel) = client.Subscribe().executeIn(this)

            responseChannel
                .consumeAsFlow()
                .map { SyncMapper.toDomain(it) }
                .collect { send(it) }
        }

    override suspend fun push(update: RemoteUpdate): Boolean =
        try {
            val response = client.PushUpdate().execute(SyncMapper.toProto(update))
            response.success
        } catch (e: Exception) {
            e.printStackTrace()
            false
        }
}
