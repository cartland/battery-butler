package com.chriscartland.batterybutler.datanetwork

import com.chriscartland.batterybutler.domain.AppLogger
import com.chriscartland.batterybutler.domain.repository.RemoteDataSource
import com.chriscartland.batterybutler.domain.repository.RemoteUpdate
import com.chriscartland.batterybutler.proto.SyncServiceClient
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.channelFlow
import kotlinx.coroutines.flow.consumeAsFlow
import me.tatarka.inject.annotations.Inject
import kotlin.coroutines.cancellation.CancellationException

@Inject
class GrpcSyncDataSource(
    private val client: SyncServiceClient,
) : RemoteDataSource {
    override fun subscribe(): Flow<RemoteUpdate> =
        channelFlow {
            val tag = "BatteryButlerGrpc"
            AppLogger.d(tag, "GrpcSyncDataSource starting subscribe...")
            // executeIn returns (SendChannel, ReceiveChannel)
            try {
                val (requestChannel, responseChannel) = client.Subscribe().executeIn(this)

                // CRITICAL: We MUST send an initial request to trigger the stream
                // Even though it's empty, the server waits for one message.
                requestChannel.send(Unit)

                responseChannel
                    .consumeAsFlow()
                    .collect { response ->
                        // println("$tag: GrpcSyncDataSource received response: $response")
                        val domainUpdate = SyncMapper.toDomain(response)
                        AppLogger.d(tag, "GrpcSyncDataSource emitting update from server")
                        send(domainUpdate)
                    }
            } catch (e: Exception) {
                if (e is CancellationException) throw e
                AppLogger.d(tag, "GrpcSyncDataSource error: $e")
                // throw e // Don't crash the flow, maybe emit error state or retry?
            }
        }

    override suspend fun push(update: RemoteUpdate): Boolean =
        try {
            val response = client.PushUpdate().execute(SyncMapper.toProto(update))
            response.success
        } catch (e: Exception) {
            if (e is CancellationException) throw e
            e.printStackTrace()
            false
        }
}
