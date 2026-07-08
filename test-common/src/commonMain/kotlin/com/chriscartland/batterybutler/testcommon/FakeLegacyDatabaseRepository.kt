package com.chriscartland.batterybutler.testcommon

import com.chriscartland.batterybutler.domain.model.DataMode
import com.chriscartland.batterybutler.domain.model.LegacyDatabaseInfo
import com.chriscartland.batterybutler.domain.model.RestoreResult
import com.chriscartland.batterybutler.domain.repository.LegacyDatabaseRepository

/**
 * Fake implementation of [LegacyDatabaseRepository] for testing.
 *
 * Provides configurable legacy database info per data mode and
 * tracks restore calls.
 *
 * Example usage:
 * ```kotlin
 * val repo = FakeLegacyDatabaseRepository()
 * repo.legacyInfoByMode[DataMode.None] = LegacyDatabaseInfo("old.db", true)
 * val info = repo.getLegacyDatabaseInfo(DataMode.None)
 * assertEquals("old.db", info?.legacyFileName)
 * ```
 */
class FakeLegacyDatabaseRepository : LegacyDatabaseRepository {
    /** Configurable legacy info responses, keyed by DataMode. */
    val legacyInfoByMode = mutableMapOf<DataMode, LegacyDatabaseInfo>()

    /** Configurable current file name responses, keyed by DataMode. */
    val fileNameByMode = mutableMapOf<DataMode, String>()

    /** Number of times [restoreLegacyDatabase] has been called. */
    var restoreCallCount = 0
        private set

    /** The last file name passed to [restoreLegacyDatabase], or null if never called. */
    var lastRestoredFileName: String? = null
        private set

    /** Result to return from [restoreLegacyDatabase]. Defaults to Success. */
    var restoreResult: RestoreResult = RestoreResult.Success

    override fun getLegacyDatabaseInfo(dataMode: DataMode): LegacyDatabaseInfo? = legacyInfoByMode[dataMode]

    override fun getCurrentDatabaseFileName(dataMode: DataMode): String = fileNameByMode[dataMode] ?: "unknown.db"

    override suspend fun restoreLegacyDatabase(legacyFileName: String): RestoreResult {
        restoreCallCount++
        lastRestoredFileName = legacyFileName
        return restoreResult
    }
}
