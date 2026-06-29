package com.chriscartland.batterybutler.datanetwork.rest

import co.touchlab.kermit.Logger
import com.chriscartland.batterybutler.datanetwork.RemoteDataSource
import com.chriscartland.batterybutler.datanetwork.RemoteDataSourceState
import com.chriscartland.batterybutler.domain.repository.RemoteUpdate
import io.ktor.client.HttpClient
import io.ktor.client.call.body
import io.ktor.client.request.bearerAuth
import io.ktor.client.request.get
import io.ktor.client.request.post
import io.ktor.client.request.setBody
import io.ktor.http.ContentType
import io.ktor.http.contentType
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.flow
import kotlin.coroutines.cancellation.CancellationException

/**
 * A [RemoteDataSource] backed by the Labs REST `/v1/battery-butler/sync` endpoint.
 *
 * Request/response, client-determined: [subscribe] does ONE GET and emits a single snapshot
 * (there is no server stream); [push] does ONE POST. Try/catch is confined to this HTTP
 * boundary (the project rule: code never throws except [CancellationException]) — a failed
 * call logs and degrades (subscribe emits nothing; push returns false) rather than crashing,
 * leaving local state untouched.
 *
 * @param tokenProvider yields a per-user Firebase ID token for the `Authorization` header;
 *   supplied by the auth layer (a later PR). Returning null sends no header.
 * @param baseUrl the env host (e.g. `https://<host>`), injected from config.
 */
internal class RestRemoteDataSource(
    private val httpClient: HttpClient,
    private val baseUrl: String,
    private val tokenProvider: suspend () -> String?,
) : RemoteDataSource {
    override val state: StateFlow<RemoteDataSourceState> =
        MutableStateFlow(
            if (baseUrl.isBlank()) {
                RemoteDataSourceState.InvalidConfiguration
            } else {
                RemoteDataSourceState.Subscribed
            },
        )

    override fun subscribe(): Flow<RemoteUpdate> =
        flow {
            try {
                val token = tokenProvider()
                val snapshot: SyncSnapshotWire =
                    httpClient.get(syncUrl()) { token?.let { bearerAuth(it) } }.body()
                emit(RestSyncMapper.toRemoteUpdate(snapshot))
            } catch (e: CancellationException) {
                throw e
            } catch (e: Exception) {
                Logger.e(TAG, e) { "subscribe() failed; leaving local state untouched" }
            }
        }

    override suspend fun push(update: RemoteUpdate): Boolean =
        try {
            val token = tokenProvider()
            val response: SyncPushResponseWire =
                httpClient
                    .post(syncUrl()) {
                        token?.let { bearerAuth(it) }
                        contentType(ContentType.Application.Json)
                        setBody(RestSyncMapper.toPushRequest(update))
                    }.body()
            response.success
        } catch (e: CancellationException) {
            throw e
        } catch (e: Exception) {
            Logger.e(TAG, e) { "push() failed" }
            false
        }

    private fun syncUrl(): String = "${baseUrl.trimEnd('/')}$SYNC_PATH"

    private companion object {
        const val TAG = "BatteryButlerRest"
        const val SYNC_PATH = "/v1/battery-butler/sync"
    }
}
