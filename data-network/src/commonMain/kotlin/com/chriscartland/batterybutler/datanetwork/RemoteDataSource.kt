package com.chriscartland.batterybutler.datanetwork

import com.chriscartland.batterybutler.domain.repository.RemoteUpdate
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.StateFlow

sealed interface RemoteDataSourceState {
    data object NotStarted : RemoteDataSourceState

    data object InvalidConfiguration : RemoteDataSourceState

    data object Subscribed : RemoteDataSourceState
}

interface RemoteDataSource {
    val state: StateFlow<RemoteDataSourceState>

    fun subscribe(): Flow<RemoteUpdate>

    suspend fun push(update: RemoteUpdate): Boolean
}
