package com.chriscartland.batterybutler.domain.repository

import com.chriscartland.batterybutler.domain.model.LegacyDatabaseInfo
import com.chriscartland.batterybutler.domain.model.NetworkMode
import com.chriscartland.batterybutler.domain.model.RestoreResult

/**
 * Provides access to legacy (pre-tag 28) database files for recovery.
 */
interface LegacyDatabaseRepository {
    /**
     * Returns information about the legacy database file for the given network mode,
     * or null if no legacy file name is associated with this mode.
     */
    fun getLegacyDatabaseInfo(networkMode: NetworkMode): LegacyDatabaseInfo?

    /**
     * Returns the current database file name for the given network mode.
     */
    fun getCurrentDatabaseFileName(networkMode: NetworkMode): String

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
