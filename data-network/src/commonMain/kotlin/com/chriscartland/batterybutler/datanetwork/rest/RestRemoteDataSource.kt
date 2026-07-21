package com.chriscartland.batterybutler.datanetwork.rest

import com.chriscartland.batterybutler.datanetwork.LabsSyncTokenSource
import com.chriscartland.batterybutler.datanetwork.LabsTokenResult
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
 * ## Retry-once on a refreshable 401
 *
 * A 401 whose reason is `expired` — or an unknown-reason 401 on a token that *locally* looked
 * unexpired (served from cache without a refresh) — earns exactly one forced token refresh and
 * one retry of the same request: the common cause is a stale token (clock skew, a rotation the
 * client missed), which a fresh token fixes without bothering the user. A 401 on a
 * freshly-minted token, a 401 with reason `invalid`, or a still-401 retry is **terminal**: it is
 * reported to [tokenSource] (which tears the session down and notifies the auth layer) and then
 * thrown as [RemoteSyncException.AuthRequired]. A token that can't be obtained for *transient*
 * reasons throws [RemoteSyncException.TokenUnavailable] instead — the sync layer surfaces that
 * as a network failure, never as "sign in required".
 *
 * @param tokenSource yields the per-user Firebase ID token for the `Authorization` header (and
 *   receives terminal-rejection reports); supplied by the auth layer.
 *   [LabsTokenResult.NoSession] means the call is refused client-side
 *   ([SyncAuthReason.NO_SESSION]) instead of firing a guaranteed-401 unauthenticated request.
 * @param baseUrl the env host (e.g. `https://<host>`), injected from config.
 */
internal class RestRemoteDataSource(
    private val httpClient: HttpClient,
    private val baseUrl: String,
    private val tokenSource: LabsSyncTokenSource,
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
            val snapshot = executeAuthed<SyncSnapshotWire> { token ->
                httpClient.get(syncUrl()) { bearerAuth(token) }
            }
            emit(RestSyncMapper.toRemoteUpdate(snapshot))
        }

    override suspend fun push(update: RemoteUpdate): Boolean {
        val pushResponse = executeAuthed<SyncPushResponseWire> { token ->
            httpClient.post(syncUrl()) {
                bearerAuth(token)
                contentType(ContentType.Application.Json)
                setBody(RestSyncMapper.toPushRequest(update))
            }
        }
        return pushResponse.success
    }

    /**
     * Runs [request] with a Bearer token, applying the retry-once policy documented on the class:
     * parse the payload on 2xx, force-refresh + retry once on a refreshable 401, report + throw on
     * a terminal 401, throw the typed wire failure on anything else.
     */
    private suspend inline fun <reified T> executeAuthed(request: (token: String) -> HttpResponse): T {
        val first = requireToken(forceRefresh = false)
        val response = request(first.idToken)
        if (response.status.isSuccess()) return response.body()
        if (response.status != HttpStatusCode.Unauthorized) {
            throw RemoteSyncException.ServerError(response.status.value)
        }

        val reason = parseAuthReason(response.bodyAsText())
        if (!shouldRetryWithForcedRefresh(reason, first.servedFromCache)) {
            tokenSource.reportSessionRejected(reason)
            throw RemoteSyncException.AuthRequired(reason)
        }

        val second = requireToken(forceRefresh = true)
        val retry = request(second.idToken)
        if (retry.status.isSuccess()) return retry.body()
        if (retry.status != HttpStatusCode.Unauthorized) {
            throw RemoteSyncException.ServerError(retry.status.value)
        }
        val retryReason = parseAuthReason(retry.bodyAsText())
        tokenSource.reportSessionRejected(retryReason)
        throw RemoteSyncException.AuthRequired(retryReason)
    }

    /**
     * Whether a 401 with [reason] on a token with [servedFromCache] earns the one forced-refresh
     * retry. `expired` always does (that is precisely what a refresh fixes). An unknown reason
     * does only when the token was served from cache — locally it looked unexpired, so the server
     * may simply know better; if we *just* minted the token, another mint can't do better and the
     * rejection is treated as authoritative. `invalid` is authoritative by definition, and
     * NO_SESSION never reaches here (no request is sent without a token).
     */
    private fun shouldRetryWithForcedRefresh(
        reason: SyncAuthReason,
        servedFromCache: Boolean,
    ): Boolean =
        when (reason) {
            SyncAuthReason.TOKEN_EXPIRED -> true
            SyncAuthReason.UNKNOWN -> servedFromCache
            SyncAuthReason.TOKEN_INVALID, SyncAuthReason.NO_SESSION -> false
        }

    private suspend fun requireToken(forceRefresh: Boolean): LabsTokenResult.Token =
        when (val result = tokenSource.getLabsToken(forceRefresh)) {
            is LabsTokenResult.Token -> result
            LabsTokenResult.NoSession -> throw RemoteSyncException.AuthRequired(SyncAuthReason.NO_SESSION)
            LabsTokenResult.SessionInvalidated -> throw RemoteSyncException.AuthRequired(SyncAuthReason.TOKEN_INVALID)
            LabsTokenResult.TransientFailure -> throw RemoteSyncException.TokenUnavailable()
        }

    private fun syncUrl(): String = "${baseUrl.trimEnd('/')}$SYNC_PATH"

    private companion object {
        const val SYNC_PATH = "/v1/battery-butler/sync"
    }
}
