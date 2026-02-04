package com.chriscartland.batterybutler.data.repository

import com.chriscartland.batterybutler.datalocal.preferences.PreferencesDataSource
import com.chriscartland.batterybutler.domain.model.NetworkMode
import com.chriscartland.batterybutler.domain.repository.NetworkModeRepository
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.map
import me.tatarka.inject.annotations.Inject

/**
 * DataStore-backed implementation of [NetworkModeRepository].
 * Persists the network mode selection across app restarts.
 */
@Inject
class DataStoreNetworkModeRepository(
    private val preferencesDataSource: PreferencesDataSource,
) : NetworkModeRepository {
    override val networkMode: Flow<NetworkMode> = preferencesDataSource.networkModeValue
        .map { value -> value?.toNetworkMode() ?: DEFAULT_MODE }

    override suspend fun setNetworkMode(mode: NetworkMode) {
        preferencesDataSource.setNetworkModeValue(mode.toStorageValue())
    }

    private companion object {
        val DEFAULT_MODE = NetworkMode.Mock

        // Storage format: "type:url" or "type" for types without URL
        private const val TYPE_MOCK = "mock"
        private const val TYPE_GRPC_LOCAL = "grpc_local"
        private const val TYPE_GRPC_AWS = "grpc_aws"
        private const val SEPARATOR = ":"

        fun String.toNetworkMode(): NetworkMode {
            val parts = split(SEPARATOR, limit = 2)
            val type = parts[0]
            val url = parts.getOrNull(1)?.takeIf { it.isNotEmpty() }

            return when (type) {
                TYPE_MOCK -> NetworkMode.Mock
                TYPE_GRPC_LOCAL -> NetworkMode.GrpcLocal(url)
                TYPE_GRPC_AWS -> NetworkMode.GrpcAws(url)
                else -> DEFAULT_MODE
            }
        }

        fun NetworkMode.toStorageValue(): String =
            when (this) {
                is NetworkMode.Mock -> TYPE_MOCK
                is NetworkMode.GrpcLocal -> "$TYPE_GRPC_LOCAL$SEPARATOR${url.orEmpty()}"
                is NetworkMode.GrpcAws -> "$TYPE_GRPC_AWS$SEPARATOR${url.orEmpty()}"
            }
    }
}
