package com.chriscartland.batterybutler.domain.repository

import com.chriscartland.batterybutler.domain.model.LegacyDatabaseInfo
import com.chriscartland.batterybutler.domain.model.NetworkMode

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
     * Restores the current database from the legacy file.
     * Closes the current DB, copies the legacy file over, then reopens.
     */
    suspend fun restoreLegacyDatabase(legacyFileName: String)
}
