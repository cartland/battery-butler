package com.chriscartland.batterybutler.domain.repository

import com.chriscartland.batterybutler.domain.model.NetworkMode
import kotlinx.coroutines.flow.Flow

interface NetworkModeRepository {
    val networkMode: Flow<NetworkMode>

    suspend fun setNetworkMode(mode: NetworkMode)
}
