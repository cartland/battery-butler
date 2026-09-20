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

    /**
     * Observe the stored app-wide list density value.
     * Returns null if no value has been stored.
     */
    val displayDensityValue: Flow<String?>

    /**
     * Store the app-wide list density value.
     */
    suspend fun setDisplayDensityValue(value: String)

    /**
     * Observe an arbitrary stored string, by key. Returns null if nothing has been stored.
     *
     * Keyed rather than one named pair per preference because the list arrangements are four
     * values per list screen, and a dedicated accessor for each would be eight near-identical
     * methods here and eight more in every fake. Callers own their key namespace.
     */
    fun stringValue(key: String): Flow<String?>

    /**
     * Store an arbitrary string under [key].
     */
    suspend fun setStringValue(
        key: String,
        value: String,
    )
}
