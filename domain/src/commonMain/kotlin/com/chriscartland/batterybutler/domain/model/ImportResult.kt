package com.chriscartland.batterybutler.domain.model

/**
 * Result of a data import operation.
 *
 * @property devicesImported Number of devices successfully imported.
 * @property deviceTypesImported Number of device types successfully imported.
 * @property eventsImported Number of battery events successfully imported.
 * @property errors Descriptions of any per-record failures encountered during import.
 */
data class ImportResult(
    val devicesImported: Int,
    val deviceTypesImported: Int,
    val eventsImported: Int,
    val errors: List<String>,
)
