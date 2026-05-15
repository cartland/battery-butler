package com.chriscartland.batterybutler.data.repository

import com.chriscartland.batterybutler.datalocal.room.DatabaseFactory
import com.chriscartland.batterybutler.datalocal.room.DatabaseOption
import com.chriscartland.batterybutler.datalocal.room.DynamicDatabaseProvider
import com.chriscartland.batterybutler.domain.model.LegacyDatabaseInfo
import com.chriscartland.batterybutler.domain.model.NetworkMode
import com.chriscartland.batterybutler.domain.model.RestoreResult
import com.chriscartland.batterybutler.domain.repository.LegacyDatabaseRepository
import me.tatarka.inject.annotations.Inject

@Inject
class DefaultLegacyDatabaseRepository(
    private val databaseFactory: DatabaseFactory,
    private val dynamicDatabaseProvider: DynamicDatabaseProvider,
) : LegacyDatabaseRepository {
    override fun getLegacyDatabaseInfo(networkMode: NetworkMode): LegacyDatabaseInfo? {
        val option = DatabaseOption.fromNetworkMode(networkMode)
        val legacyFileName = DatabaseOption.legacyFileNames[option] ?: return null
        return LegacyDatabaseInfo(
            legacyFileName = legacyFileName,
            exists = databaseFactory.databaseFileExists(legacyFileName),
        )
    }

    override fun getCurrentDatabaseFileName(networkMode: NetworkMode): String = DatabaseOption.fromNetworkMode(networkMode).fileName

    override suspend fun restoreLegacyDatabase(legacyFileName: String): RestoreResult = dynamicDatabaseProvider.restoreFromLegacy(legacyFileName)
}
