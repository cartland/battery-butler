package com.chriscartland.batterybutler.datanetwork

import com.chriscartland.batterybutler.domain.repository.RemoteUpdate
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.flowOf

data object NoOpRemoteDataSource : RemoteDataSource {
    override val state: StateFlow<RemoteDataSourceState> =
        MutableStateFlow(RemoteDataSourceState.NotStarted)

    override fun subscribe(): Flow<RemoteUpdate> = flowOf()

    override suspend fun push(update: RemoteUpdate): Boolean = true
}
