package com.chriscartland.batterybutler.datanetwork

import co.touchlab.kermit.Logger
import com.chriscartland.batterybutler.datanetwork.grpc.DelegatingGrpcClient
import com.chriscartland.batterybutler.datanetwork.grpc.GrpcClientState
import com.chriscartland.batterybutler.datanetwork.rest.RestRemoteDataSource
import com.chriscartland.batterybutler.datanetwork.rest.createSyncHttpClient
import com.chriscartland.batterybutler.domain.model.DataMode
import com.chriscartland.batterybutler.domain.repository.DataModeRepository
import com.chriscartland.batterybutler.domain.repository.RemoteUpdate
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.emitAll
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.flow.flatMapLatest
import kotlinx.coroutines.flow.flow
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.flow.stateIn
import me.tatarka.inject.annotations.Inject

@Inject
@OptIn(ExperimentalCoroutinesApi::class)
class DelegatingRemoteDataSource(
    private val mockDataSource: MockRemoteDataSource,
    private val grpcDataSource: GrpcSyncDataSource,
    private val delegatingGrpcClient: DelegatingGrpcClient,
    private val dataModeRepository: DataModeRepository,
    private val labsAuthGateway: LabsAuthGateway,
    private val scope: CoroutineScope,
) : RemoteDataSource {
    // Lazily created on first use of a Labs (REST) mode. createSyncHttpClient() is internal to
    // data-network, so no DI wiring is needed for it.
    private val syncHttpClient by lazy { createSyncHttpClient() }

    // A REST data source for one Labs env URL. The URL is null until Workstream E injects the host
    // (blank -> InvalidConfiguration); the Bearer token comes from the shared Labs session held by
    // the singleton [labsAuthGateway] (the LabsSyncTokenSource seam), which also receives
    // terminal-401 reports so a dead session is torn down reactively.
    private fun restDataSource(url: String?): RestRemoteDataSource =
        RestRemoteDataSource(
            httpClient = syncHttpClient,
            baseUrl = url.orEmpty(),
            tokenSource = labsAuthGateway,
        )

    override val state: StateFlow<RemoteDataSourceState> =
        dataModeRepository.dataMode
            .flatMapLatest { mode ->
                when (mode) {
                    DataMode.None -> {
                        kotlinx.coroutines.flow.flowOf(RemoteDataSourceState.NotStarted)
                    }

                    DataMode.Mock -> {
                        mockDataSource.state
                    }

                    is DataMode.GrpcLocal, is DataMode.GrpcAws, is DataMode.GrpcDev -> {
                        delegatingGrpcClient.clientState.map { clientState ->
                            when (clientState) {
                                GrpcClientState.Uninitialized -> RemoteDataSourceState.NotStarted
                                GrpcClientState.InvalidConfiguration -> RemoteDataSourceState.InvalidConfiguration
                                is GrpcClientState.Ready -> RemoteDataSourceState.Subscribed
                            }
                        }
                    }

                    is DataMode.LabsStaging -> {
                        restDataSource(mode.url).state
                    }

                    is DataMode.LabsProd -> {
                        restDataSource(mode.url).state
                    }
                }
            }.stateIn(scope, kotlinx.coroutines.flow.SharingStarted.Eagerly, RemoteDataSourceState.NotStarted)

    /**
     * One subscription against the data source for the mode selected **at collection time**.
     *
     * Deliberately NOT mode-reactive: the mode is read once per collection and the returned flow
     * **completes when the underlying source's flow completes** (a Labs REST fetch emits one
     * snapshot and completes; a gRPC stream completes on disconnect). Mode reactivity lives in
     * exactly one place — `DefaultSyncManager`'s `collectLatest` over `dataMode` — which cancels
     * and re-subscribes on a mode change.
     *
     * The previous shape (`dataMode.flatMapLatest { ... }`) starved the sync loop in production:
     * `flatMapLatest` swallows the inner flow's completion, so after the one-shot REST fetch
     * completed, the collector just waited for a `dataMode` re-emission that (post-#1379
     * `distinctUntilChanged`) never came — one successful sync, then zero requests forever. It
     * also made every collector implicitly mode-reactive, which is how a spurious `dataMode`
     * re-emission could cancel an in-flight snapshot apply (`bb-signin-empty-list`). See
     * `bb-sync-loop-starvation` in TODO.md.
     */
    override fun subscribe(): Flow<RemoteUpdate> =
        flow {
            when (val mode = dataModeRepository.dataMode.first()) {
                DataMode.None -> {
                    Unit
                }

                // nothing to sync; complete immediately

                DataMode.Mock -> {
                    emitAll(mockDataSource.subscribe())
                }

                is DataMode.GrpcLocal, is DataMode.GrpcAws, is DataMode.GrpcDev -> {
                    // Wait for the client to be ready, then hand the stream through. A client
                    // swap mid-stream ends the old stream; the sync loop reconnects and picks
                    // up the new client here.
                    delegatingGrpcClient.clientState.first { it is GrpcClientState.Ready }
                    emitAll(grpcDataSource.subscribe())
                }

                is DataMode.LabsStaging -> {
                    emitAll(restDataSource(mode.url).subscribe())
                }

                is DataMode.LabsProd -> {
                    emitAll(restDataSource(mode.url).subscribe())
                }
            }
        }

    override suspend fun push(update: RemoteUpdate): Boolean {
        val mode = dataModeRepository.dataMode.first()
        Logger.d("DelegatingRemoteDS") { "push() called with mode=$mode" }
        return when (mode) {
            DataMode.None -> {
                Logger.d("DelegatingRemoteDS") { "Pushing to None (no-op)" }
                true
            }

            DataMode.Mock -> {
                Logger.d("DelegatingRemoteDS") { "Pushing to Mock (no-op)" }
                mockDataSource.push(update)
            }

            is DataMode.GrpcLocal, is DataMode.GrpcAws, is DataMode.GrpcDev -> {
                // Wait for the client to be ready before pushing
                Logger.d("DelegatingRemoteDS") { "Waiting for gRPC client to be ready..." }
                delegatingGrpcClient.clientState.first { it is GrpcClientState.Ready }
                Logger.d("DelegatingRemoteDS") { "gRPC client ready, pushing update..." }
                val success = grpcDataSource.push(update)
                Logger.d("DelegatingRemoteDS") { "Push result: success=$success" }
                success
            }

            is DataMode.LabsStaging -> {
                restDataSource(mode.url).push(update)
            }

            is DataMode.LabsProd -> {
                restDataSource(mode.url).push(update)
            }
        }
    }
}
