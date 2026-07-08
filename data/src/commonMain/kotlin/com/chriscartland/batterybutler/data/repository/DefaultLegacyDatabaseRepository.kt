package com.chriscartland.batterybutler.data.repository

import com.chriscartland.batterybutler.datalocal.room.DatabaseFactory
import com.chriscartland.batterybutler.datalocal.room.DatabaseOption
import com.chriscartland.batterybutler.datalocal.room.DynamicDatabaseProvider
import com.chriscartland.batterybutler.domain.model.DataMode
import com.chriscartland.batterybutler.domain.model.LegacyDatabaseInfo
import com.chriscartland.batterybutler.domain.model.RestoreResult
import com.chriscartland.batterybutler.domain.repository.LegacyDatabaseRepository
import me.tatarka.inject.annotations.Inject

@Inject
class DefaultLegacyDatabaseRepository(
    private val databaseFactory: DatabaseFactory,
    private val dynamicDatabaseProvider: DynamicDatabaseProvider,
) : LegacyDatabaseRepository {
    override fun getLegacyDatabaseInfo(dataMode: DataMode): LegacyDatabaseInfo? {
        val option = DatabaseOption.fromDataMode(dataMode)
        val legacyFileName = DatabaseOption.legacyFileNames[option.category] ?: return null
        return LegacyDatabaseInfo(
            legacyFileName = legacyFileName,
            exists = databaseFactory.databaseFileExists(legacyFileName),
        )
    }

    override fun getCurrentDatabaseFileName(dataMode: DataMode): String = DatabaseOption.fromDataMode(dataMode).fileName

    override suspend fun restoreLegacyDatabase(legacyFileName: String): RestoreResult = dynamicDatabaseProvider.restoreFromLegacy(legacyFileName)
}
