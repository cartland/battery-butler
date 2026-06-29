package com.chriscartland.batterybutler.datanetwork

import co.touchlab.kermit.Logger
import com.chriscartland.batterybutler.datanetwork.grpc.DelegatingGrpcClient
import com.chriscartland.batterybutler.datanetwork.grpc.GrpcClientState
import com.chriscartland.batterybutler.datanetwork.rest.RestRemoteDataSource
import com.chriscartland.batterybutler.datanetwork.rest.createSyncHttpClient
import com.chriscartland.batterybutler.domain.model.NetworkMode
import com.chriscartland.batterybutler.domain.repository.NetworkModeRepository
import com.chriscartland.batterybutler.domain.repository.RemoteUpdate
import com.squareup.wire.GrpcClient
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.flow.flatMapLatest
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.flow.mapNotNull
import kotlinx.coroutines.flow.stateIn
import me.tatarka.inject.annotations.Inject

@Inject
@OptIn(ExperimentalCoroutinesApi::class)
class DelegatingRemoteDataSource(
    private val mockDataSource: MockRemoteDataSource,
    private val grpcDataSource: GrpcSyncDataSource,
    private val delegatingGrpcClient: DelegatingGrpcClient,
    private val networkModeRepository: NetworkModeRepository,
    private val scope: CoroutineScope,
) : RemoteDataSource {
    // Lazily created on first use of a Labs (REST) mode. createSyncHttpClient() is internal to
    // data-network, so no DI wiring is needed for it.
    private val syncHttpClient by lazy { createSyncHttpClient() }

    // A REST data source for one Labs env URL. The token is a placeholder until Workstream D
    // wires a per-user Firebase ID token; the URL is null until Workstream E injects the host —
    // so a Labs mode is selectable but reports unavailable / unauthenticated until both land.
    private fun restDataSource(url: String?): RestRemoteDataSource =
        RestRemoteDataSource(
            httpClient = syncHttpClient,
            baseUrl = url.orEmpty(),
            tokenProvider = { null },
        )

    override val state: StateFlow<RemoteDataSourceState> =
        networkModeRepository.networkMode
            .flatMapLatest { mode ->
                when (mode) {
                    NetworkMode.None -> {
                        kotlinx.coroutines.flow.flowOf(RemoteDataSourceState.NotStarted)
                    }

                    NetworkMode.Mock -> {
                        mockDataSource.state
                    }

                    is NetworkMode.GrpcLocal, is NetworkMode.GrpcAws, is NetworkMode.GrpcDev -> {
                        delegatingGrpcClient.clientState.map { clientState ->
                            when (clientState) {
                                GrpcClientState.Uninitialized -> RemoteDataSourceState.NotStarted
                                GrpcClientState.InvalidConfiguration -> RemoteDataSourceState.InvalidConfiguration
                                is GrpcClientState.Ready -> RemoteDataSourceState.Subscribed
                            }
                        }
                    }

                    is NetworkMode.LabsStaging -> {
                        restDataSource(mode.url).state
                    }

                    is NetworkMode.LabsProd -> {
                        restDataSource(mode.url).state
                    }
                }
            }.stateIn(scope, kotlinx.coroutines.flow.SharingStarted.Eagerly, RemoteDataSourceState.NotStarted)

    @OptIn(ExperimentalCoroutinesApi::class)
    override fun subscribe(): Flow<RemoteUpdate> =
        networkModeRepository.networkMode.flatMapLatest { mode ->
            when (mode) {
                NetworkMode.None -> {
                    kotlinx.coroutines.flow.emptyFlow()
                }

                NetworkMode.Mock -> {
                    mockDataSource.subscribe()
                }

                is NetworkMode.GrpcLocal -> {
                    // Wait for the client to be ready
                    delegatingGrpcClient.clientState
                        .mapNotNull { (it as? GrpcClientState.Ready)?.client }
                        .flatMapLatest<GrpcClient, RemoteUpdate> { grpcDataSource.subscribe() }
                }

                is NetworkMode.GrpcAws, is NetworkMode.GrpcDev -> {
                    // Wait for the client to be ready
                    delegatingGrpcClient.clientState
                        .mapNotNull { (it as? GrpcClientState.Ready)?.client }
                        .flatMapLatest<GrpcClient, RemoteUpdate> { grpcDataSource.subscribe() }
                }

                is NetworkMode.LabsStaging -> {
                    restDataSource(mode.url).subscribe()
                }

                is NetworkMode.LabsProd -> {
                    restDataSource(mode.url).subscribe()
                }
            }
        }

    override suspend fun push(update: RemoteUpdate): Boolean {
        val mode = networkModeRepository.networkMode.first()
        Logger.d("DelegatingRemoteDS") { "push() called with mode=$mode" }
        return when (mode) {
            NetworkMode.None -> {
                Logger.d("DelegatingRemoteDS") { "Pushing to None (no-op)" }
                true
            }

            NetworkMode.Mock -> {
                Logger.d("DelegatingRemoteDS") { "Pushing to Mock (no-op)" }
                mockDataSource.push(update)
            }

            is NetworkMode.GrpcLocal, is NetworkMode.GrpcAws, is NetworkMode.GrpcDev -> {
                // Wait for the client to be ready before pushing
                Logger.d("DelegatingRemoteDS") { "Waiting for gRPC client to be ready..." }
                delegatingGrpcClient.clientState.first { it is GrpcClientState.Ready }
                Logger.d("DelegatingRemoteDS") { "gRPC client ready, pushing update..." }
                val success = grpcDataSource.push(update)
                Logger.d("DelegatingRemoteDS") { "Push result: success=$success" }
                success
            }

            is NetworkMode.LabsStaging -> {
                restDataSource(mode.url).push(update)
            }

            is NetworkMode.LabsProd -> {
                restDataSource(mode.url).push(update)
            }
        }
    }
}
