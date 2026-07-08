package com.chriscartland.batterybutler.datalocal.preferences

import kotlinx.coroutines.flow.Flow

/**
 * Interface for persisting app preferences.
 */
interface PreferencesDataSource {
    /**
     * Observe the stored data mode value.
     * Returns null if no value has been stored.
     */
    val dataModeValue: Flow<String?>

    /**
     * Store the data mode value.
     */
    suspend fun setDataModeValue(value: String)
}
