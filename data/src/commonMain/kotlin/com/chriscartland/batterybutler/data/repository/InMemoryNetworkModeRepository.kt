package com.chriscartland.batterybutler.data.repository

import com.chriscartland.batterybutler.domain.model.NetworkMode
import com.chriscartland.batterybutler.domain.repository.NetworkModeRepository
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.asStateFlow
import me.tatarka.inject.annotations.Inject

@Inject
class InMemoryNetworkModeRepository : NetworkModeRepository {
    private val _networkMode = MutableStateFlow<NetworkMode>(NetworkMode.Mock)
    override val networkMode: Flow<NetworkMode> = _networkMode.asStateFlow()

    override suspend fun setNetworkMode(mode: NetworkMode) {
        _networkMode.value = mode
    }
}
