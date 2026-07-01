package com.chriscartland.batterybutler.data.repository

import com.chriscartland.batterybutler.datalocal.preferences.PreferencesDataSource
import com.chriscartland.batterybutler.domain.model.LabsStagingUrl
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
    private val labsStagingUrl: LabsStagingUrl,
) : NetworkModeRepository {
    /**
     * Backend used on a fresh install (no stored selection yet). Defaults to **Labs staging** when
     * its host is configured (the Workstream E secret) so the app is Labs-first — the front-door
     * sign-in is the Labs sign-in on first launch. Falls back to local-only [NetworkMode.None] when
     * the Labs host isn't configured (e.g. a secret-less build), so those builds stay safe/offline.
     * (Will point at Labs prod once prod is set up.)
     */
    private val defaultMode: NetworkMode =
        labsStagingUrl.url
            .ifBlank { null }
            ?.let { NetworkMode.LabsStaging(it) }
            ?: NetworkMode.None

    override val networkMode: Flow<NetworkMode> = preferencesDataSource.networkModeValue
        .map { value -> value?.toNetworkMode(defaultMode) ?: defaultMode }

    override suspend fun setNetworkMode(mode: NetworkMode) {
        preferencesDataSource.setNetworkModeValue(mode.toStorageValue())
    }

    private companion object {
        // Storage format: "type:url" or "type" for types without URL
        private const val TYPE_MOCK = "mock"
        private const val TYPE_NONE = "none"
        private const val TYPE_GRPC_LOCAL = "grpc_local"
        private const val TYPE_GRPC_AWS = "grpc_aws"
        private const val TYPE_GRPC_DEV = "grpc_dev"
        private const val TYPE_LABS_STAGING = "labs_staging"
        private const val TYPE_LABS_PROD = "labs_prod"
        private const val SEPARATOR = ":"

        fun String.toNetworkMode(default: NetworkMode): NetworkMode {
            val parts = split(SEPARATOR, limit = 2)
            val type = parts[0]
            val url = parts.getOrNull(1)?.takeIf { it.isNotEmpty() }

            return when (type) {
                TYPE_MOCK -> NetworkMode.Mock
                TYPE_NONE -> NetworkMode.None
                TYPE_GRPC_LOCAL -> NetworkMode.GrpcLocal(url)
                TYPE_GRPC_AWS -> NetworkMode.GrpcAws(url)
                TYPE_GRPC_DEV -> NetworkMode.GrpcDev(url)
                TYPE_LABS_STAGING -> NetworkMode.LabsStaging(url)
                TYPE_LABS_PROD -> NetworkMode.LabsProd(url)
                else -> default
            }
        }

        fun NetworkMode.toStorageValue(): String =
            when (this) {
                is NetworkMode.Mock -> TYPE_MOCK
                is NetworkMode.None -> TYPE_NONE
                is NetworkMode.GrpcLocal -> "$TYPE_GRPC_LOCAL$SEPARATOR${url.orEmpty()}"
                is NetworkMode.GrpcAws -> "$TYPE_GRPC_AWS$SEPARATOR${url.orEmpty()}"
                is NetworkMode.GrpcDev -> "$TYPE_GRPC_DEV$SEPARATOR${url.orEmpty()}"
                is NetworkMode.LabsStaging -> "$TYPE_LABS_STAGING$SEPARATOR${url.orEmpty()}"
                is NetworkMode.LabsProd -> "$TYPE_LABS_PROD$SEPARATOR${url.orEmpty()}"
            }
    }
}
