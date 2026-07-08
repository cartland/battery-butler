package com.chriscartland.batterybutler.domain.repository

import com.chriscartland.batterybutler.domain.model.DataMode
import com.chriscartland.batterybutler.domain.model.LegacyDatabaseInfo
import com.chriscartland.batterybutler.domain.model.RestoreResult

/**
 * Provides access to legacy (pre-tag 28) database files for recovery.
 */
interface LegacyDatabaseRepository {
    /**
     * Returns information about the legacy database file for the given data mode,
     * or null if no legacy file name is associated with this mode.
     */
    fun getLegacyDatabaseInfo(dataMode: DataMode): LegacyDatabaseInfo?

    /**
     * Returns the current database file name for the given data mode.
     */
    fun getCurrentDatabaseFileName(dataMode: DataMode): String

    /**
     * Restores the current database from the legacy file. The active database
     * file is backed up before the legacy file is copied over it; on any
     * failure the backup is restored so the app is never left with a broken
     * active database.
     *
     * The returned [RestoreResult] is exhaustive — the function does not throw.
     */
    suspend fun restoreLegacyDatabase(legacyFileName: String): RestoreResult
}
