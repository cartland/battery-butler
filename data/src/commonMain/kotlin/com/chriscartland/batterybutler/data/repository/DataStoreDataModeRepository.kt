package com.chriscartland.batterybutler.data.repository

import com.chriscartland.batterybutler.datalocal.preferences.PreferencesDataSource
import com.chriscartland.batterybutler.domain.model.DataMode
import com.chriscartland.batterybutler.domain.model.LabsProdUrl
import com.chriscartland.batterybutler.domain.model.LabsStagingUrl
import com.chriscartland.batterybutler.domain.repository.DataModeRepository
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.map
import me.tatarka.inject.annotations.Inject

/**
 * The backend a fresh install defaults to (no stored selection yet), given the configured Labs
 * hosts. **Prefers prod**: Labs **prod** when its host is configured, else Labs **staging** when
 * configured, else local-only [DataMode.None]. Prod-first so a configured release ships pointing
 * at prod (not staging); a secret-less build stays safe/offline. Pure + [internal] so it's unit-
 * tested directly (see `DefaultDataModeTest`).
 */
internal fun defaultDataMode(
    prodUrl: String,
    stagingUrl: String,
): DataMode =
    prodUrl.ifBlank { null }?.let { DataMode.LabsProd(it) }
        ?: stagingUrl.ifBlank { null }?.let { DataMode.LabsStaging(it) }
        ?: DataMode.None

/**
 * DataStore-backed implementation of [DataModeRepository].
 * Persists the data mode selection across app restarts.
 */
@Inject
class DataStoreDataModeRepository(
    private val preferencesDataSource: PreferencesDataSource,
    private val labsStagingUrl: LabsStagingUrl,
    private val labsProdUrl: LabsProdUrl,
) : DataModeRepository {
    // Fresh-install default: Labs prod when configured, else staging, else offline (see
    // [defaultDataMode]). Prod-first so a configured release defaults real users to prod.
    private val defaultMode: DataMode = defaultDataMode(labsProdUrl.url, labsStagingUrl.url)

    override val dataMode: Flow<DataMode> = preferencesDataSource.dataModeValue
        .map { value -> value?.toDataMode(defaultMode) ?: defaultMode }

    override suspend fun setDataMode(mode: DataMode) {
        preferencesDataSource.setDataModeValue(mode.toStorageValue())
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

        fun String.toDataMode(default: DataMode): DataMode {
            val parts = split(SEPARATOR, limit = 2)
            val type = parts[0]
            val url = parts.getOrNull(1)?.takeIf { it.isNotEmpty() }

            return when (type) {
                TYPE_MOCK -> DataMode.Mock
                TYPE_NONE -> DataMode.None
                TYPE_GRPC_LOCAL -> DataMode.GrpcLocal(url)
                TYPE_GRPC_AWS -> DataMode.GrpcAws(url)
                TYPE_GRPC_DEV -> DataMode.GrpcDev(url)
                TYPE_LABS_STAGING -> DataMode.LabsStaging(url)
                TYPE_LABS_PROD -> DataMode.LabsProd(url)
                else -> default
            }
        }

        fun DataMode.toStorageValue(): String =
            when (this) {
                is DataMode.Mock -> TYPE_MOCK
                is DataMode.None -> TYPE_NONE
                is DataMode.GrpcLocal -> "$TYPE_GRPC_LOCAL$SEPARATOR${url.orEmpty()}"
                is DataMode.GrpcAws -> "$TYPE_GRPC_AWS$SEPARATOR${url.orEmpty()}"
                is DataMode.GrpcDev -> "$TYPE_GRPC_DEV$SEPARATOR${url.orEmpty()}"
                is DataMode.LabsStaging -> "$TYPE_LABS_STAGING$SEPARATOR${url.orEmpty()}"
                is DataMode.LabsProd -> "$TYPE_LABS_PROD$SEPARATOR${url.orEmpty()}"
            }
    }
}
