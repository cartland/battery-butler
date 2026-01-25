package com.chriscartland.batterybutler.datanetwork

import co.touchlab.kermit.Logger
import com.chriscartland.batterybutler.datanetwork.grpc.DelegatingGrpcClient
import com.chriscartland.batterybutler.datanetwork.grpc.GrpcClientState
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
    override val state: StateFlow<RemoteDataSourceState> =
        networkModeRepository.networkMode
            .flatMapLatest { mode ->
                when (mode) {
                    NetworkMode.Mock -> mockDataSource.state
                    is NetworkMode.GrpcLocal, is NetworkMode.GrpcAws -> {
                        delegatingGrpcClient.clientState.map { clientState ->
                            when (clientState) {
                                GrpcClientState.Uninitialized -> RemoteDataSourceState.NotStarted
                                GrpcClientState.InvalidConfiguration -> RemoteDataSourceState.InvalidConfiguration
                                is GrpcClientState.Ready -> RemoteDataSourceState.Subscribed
                            }
                        }
                    }
                }
            }.stateIn(scope, kotlinx.coroutines.flow.SharingStarted.Eagerly, RemoteDataSourceState.NotStarted)

    @OptIn(ExperimentalCoroutinesApi::class)
    override fun subscribe(): Flow<RemoteUpdate> =
        networkModeRepository.networkMode.flatMapLatest { mode ->
            when (mode) {
                NetworkMode.Mock -> mockDataSource.subscribe()
                is NetworkMode.GrpcLocal -> {
                    // Wait for the client to be ready
                    delegatingGrpcClient.clientState
                        .mapNotNull { (it as? GrpcClientState.Ready)?.client }
                        .flatMapLatest<GrpcClient, RemoteUpdate> { grpcDataSource.subscribe() }
                }
                is NetworkMode.GrpcAws -> {
                    // Wait for the client to be ready
                    delegatingGrpcClient.clientState
                        .mapNotNull { (it as? GrpcClientState.Ready)?.client }
                        .flatMapLatest<GrpcClient, RemoteUpdate> { grpcDataSource.subscribe() }
                }
            }
        }

    override suspend fun push(update: RemoteUpdate): Boolean {
        val mode = networkModeRepository.networkMode.first()
        Logger.d("DelegatingRemoteDS") { "push() called with mode=$mode" }
        return when (mode) {
            NetworkMode.Mock -> {
                Logger.d("DelegatingRemoteDS") { "Pushing to Mock (no-op)" }
                mockDataSource.push(update)
            }
            is NetworkMode.GrpcLocal, is NetworkMode.GrpcAws -> {
                // Wait for the client to be ready before pushing
                Logger.d("DelegatingRemoteDS") { "Waiting for gRPC client to be ready..." }
                delegatingGrpcClient.clientState.first { it is GrpcClientState.Ready }
                Logger.d("DelegatingRemoteDS") { "gRPC client ready, pushing update..." }
                val success = grpcDataSource.push(update)
                Logger.d("DelegatingRemoteDS") { "Push result: success=$success" }
                success
            }
        }
    }
}
