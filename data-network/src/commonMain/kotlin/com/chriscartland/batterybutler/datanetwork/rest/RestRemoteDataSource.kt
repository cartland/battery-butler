package com.chriscartland.batterybutler.datanetwork.rest

import com.chriscartland.batterybutler.datanetwork.RemoteDataSource
import com.chriscartland.batterybutler.datanetwork.RemoteDataSourceState
import com.chriscartland.batterybutler.datanetwork.RemoteSyncException
import com.chriscartland.batterybutler.domain.model.SyncAuthReason
import com.chriscartland.batterybutler.domain.repository.RemoteUpdate
import io.ktor.client.HttpClient
import io.ktor.client.call.body
import io.ktor.client.request.bearerAuth
import io.ktor.client.request.get
import io.ktor.client.request.post
import io.ktor.client.request.setBody
import io.ktor.client.statement.HttpResponse
import io.ktor.client.statement.bodyAsText
import io.ktor.http.ContentType
import io.ktor.http.HttpStatusCode
import io.ktor.http.contentType
import io.ktor.http.isSuccess
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.flow

/**
 * A [RemoteDataSource] backed by the Labs REST `/v1/battery-butler/sync` endpoint.
 *
 * Request/response, client-determined: [subscribe] does ONE GET and emits a single snapshot
 * (there is no server stream); [push] does ONE POST.
 *
 * Wire honesty: only a 2xx response is parsed as a payload. A 401 surfaces as
 * [RemoteSyncException.AuthRequired] (with the reason parsed leniently from the Labs error
 * envelope — see [parseAuthReason]); any other non-2xx surfaces as
 * [RemoteSyncException.ServerError]. Without this branching, the backend's 401 JSON error body
 * deserialized cleanly into [SyncSnapshotWire] (every field defaulted) and masqueraded as an
 * empty-but-successful snapshot. Failures are *thrown* (through [subscribe]'s flow, or from
 * [push]) rather than swallowed here: the sync manager's loop is the single catch point, where
 * they become a visible [com.chriscartland.batterybutler.domain.model.SyncStatus] instead of a
 * silent no-op. Local state is never touched by a failed call.
 *
 * @param tokenProvider yields a per-user Firebase ID token for the `Authorization` header;
 *   supplied by the auth layer. Yielding null means "no session": the call is refused
 *   client-side ([SyncAuthReason.NO_SESSION]) instead of firing a guaranteed-401
 *   unauthenticated request.
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
            val token = requireToken()
            val response = httpClient.get(syncUrl()) { bearerAuth(token) }
            val snapshot: SyncSnapshotWire = response.bodyOrThrow()
            emit(RestSyncMapper.toRemoteUpdate(snapshot))
        }

    override suspend fun push(update: RemoteUpdate): Boolean {
        val token = requireToken()
        val response = httpClient.post(syncUrl()) {
            bearerAuth(token)
            contentType(ContentType.Application.Json)
            setBody(RestSyncMapper.toPushRequest(update))
        }
        val pushResponse: SyncPushResponseWire = response.bodyOrThrow()
        return pushResponse.success
    }

    private suspend fun requireToken(): String = tokenProvider() ?: throw RemoteSyncException.AuthRequired(SyncAuthReason.NO_SESSION)

    /** Parses the payload on 2xx; throws the typed wire failure for any other status. */
    private suspend inline fun <reified T> HttpResponse.bodyOrThrow(): T =
        when {
            status.isSuccess() -> {
                body()
            }

            status == HttpStatusCode.Unauthorized -> {
                throw RemoteSyncException.AuthRequired(parseAuthReason(bodyAsText()))
            }

            else -> {
                throw RemoteSyncException.ServerError(status.value)
            }
        }

    private fun syncUrl(): String = "${baseUrl.trimEnd('/')}$SYNC_PATH"

    private companion object {
        const val SYNC_PATH = "/v1/battery-butler/sync"
    }
}
