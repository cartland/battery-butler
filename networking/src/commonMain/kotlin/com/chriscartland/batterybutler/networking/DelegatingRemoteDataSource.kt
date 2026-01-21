package com.chriscartland.batterybutler.networking

import com.chriscartland.batterybutler.domain.model.NetworkMode
import com.chriscartland.batterybutler.domain.repository.RemoteDataSource
import com.chriscartland.batterybutler.domain.repository.RemoteUpdate
import com.squareup.wire.GrpcClient
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.filterNotNull
import kotlinx.coroutines.flow.flatMapLatest
import kotlinx.coroutines.flow.stateIn
import me.tatarka.inject.annotations.Inject

@Inject
class DelegatingRemoteDataSource(
    private val mockDataSource: MockRemoteDataSource,
    private val grpcDataSource: GrpcSyncDataSource,
    private val delegatingGrpcClient: DelegatingGrpcClient,
    private val networkMode: Flow<NetworkMode>,
    private val scope: CoroutineScope,
) : RemoteDataSource {
    @OptIn(ExperimentalCoroutinesApi::class)
    override fun subscribe(): Flow<RemoteUpdate> =
        networkMode.flatMapLatest { mode ->
            when (mode) {
                NetworkMode.MOCK -> mockDataSource.subscribe()
                NetworkMode.GRPC_LOCAL -> {
                    // Wait for the client to be ready
                    delegatingGrpcClient.clientState
                        .filterNotNull()
                        .flatMapLatest<GrpcClient, RemoteUpdate> { grpcDataSource.subscribe() }
                }
            }
        }

    override suspend fun push(update: RemoteUpdate): Boolean {
        // We need the current mode.
        // Since we don't have direct access to value here without collecting,
        // we can assume the repository holding the mode behaves like a StateFlow,
        // OR we just collect first.
        // But better is if networkMode IS a StateFlow or we cache it.
        // For simplicity, we just collect the latest value.

        // Optimally, we'd use a stateIn property.
        val currentMode = currentNetworkMode.value
        return when (currentMode) {
            NetworkMode.MOCK -> mockDataSource.push(update)
            NetworkMode.GRPC_LOCAL -> grpcDataSource.push(update)
        }
    }

    private val currentNetworkMode: StateFlow<NetworkMode> = networkMode.stateIn(
        scope,
        SharingStarted.Eagerly,
        NetworkMode.MOCK,
    )
}
