package com.chriscartland.batterybutler.domain.model

/**
 * Input data for creating a new device type.
 *
 * @property name Display name for the device type (1-100 characters).
 * @property defaultIcon Optional icon identifier.
 * @property batteryType Battery type designation (1-50 characters, e.g., "AA", "AAA", "CR2032").
 * @property batteryQuantity Number of batteries required (1-99).
 * @throws IllegalArgumentException if validation fails.
 */
data class DeviceTypeInput(
    val name: String,
    val defaultIcon: String?,
    val batteryType: String,
    val batteryQuantity: Int,
) {
    init {
        require(name.isNotBlank()) { "Device type name cannot be blank" }
        require(name.length <= MAX_NAME_LENGTH) { "Device type name exceeds $MAX_NAME_LENGTH characters" }
        require(batteryType.isNotBlank()) { "Battery type cannot be blank" }
        require(batteryType.length <= MAX_BATTERY_TYPE_LENGTH) {
            "Battery type exceeds $MAX_BATTERY_TYPE_LENGTH characters"
        }
        require(batteryQuantity in MIN_BATTERY_QUANTITY..MAX_BATTERY_QUANTITY) {
            "Battery quantity must be between $MIN_BATTERY_QUANTITY and $MAX_BATTERY_QUANTITY"
        }
    }

    companion object {
        const val MAX_NAME_LENGTH = 100
        const val MAX_BATTERY_TYPE_LENGTH = 50
        const val MIN_BATTERY_QUANTITY = 1
        const val MAX_BATTERY_QUANTITY = 99
    }
}
