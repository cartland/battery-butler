package com.chriscartland.batterybutler.testcommon

import com.chriscartland.batterybutler.domain.model.NetworkMode
import com.chriscartland.batterybutler.domain.repository.NetworkModeRepository
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.MutableStateFlow

/**
 * Fake implementation of [NetworkModeRepository] for testing.
 *
 * Backed by a [MutableStateFlow] for reactive updates.
 *
 * Example usage:
 * ```kotlin
 * val repo = FakeNetworkModeRepository()
 * repo.setNetworkMode(NetworkMode.Mock)
 * assertEquals(NetworkMode.Mock, repo.networkMode.first())
 * ```
 */
class FakeNetworkModeRepository(
    initialMode: NetworkMode = NetworkMode.None,
) : NetworkModeRepository {
    private val _networkMode = MutableStateFlow(initialMode)

    override val networkMode: Flow<NetworkMode> = _networkMode

    override suspend fun setNetworkMode(mode: NetworkMode) {
        _networkMode.value = mode
    }
}
