package com.chriscartland.batterybutler.datalocal.preferences

import kotlinx.coroutines.flow.Flow

/**
 * Interface for persisting app preferences.
 */
interface PreferencesDataSource {
    /**
     * Observe the stored network mode value.
     * Returns null if no value has been stored.
     */
    val networkModeValue: Flow<String?>

    /**
     * Store the network mode value.
     */
    suspend fun setNetworkModeValue(value: String)
}
