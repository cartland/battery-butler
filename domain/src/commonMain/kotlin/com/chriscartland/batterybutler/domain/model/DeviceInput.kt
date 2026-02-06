package com.chriscartland.batterybutler.domain.model

/**
 * Input data for creating a new device.
 *
 * @property name User-facing display name for the device (1-100 characters).
 * @property location Optional location description (max 100 characters).
 * @property typeId Reference to the DeviceType.
 * @property imagePath Optional path to a device image.
 * @throws IllegalArgumentException if validation fails.
 */
data class DeviceInput(
    val name: String,
    val location: String?,
    val typeId: String,
    val imagePath: String? = null,
) {
    init {
        require(name.isNotBlank()) { "Device name cannot be blank" }
        require(name.length <= MAX_NAME_LENGTH) { "Device name exceeds $MAX_NAME_LENGTH characters" }
        require(typeId.isNotBlank()) { "Device type ID cannot be blank" }
        location?.let {
            require(it.length <= MAX_LOCATION_LENGTH) { "Location exceeds $MAX_LOCATION_LENGTH characters" }
        }
    }

    companion object {
        const val MAX_NAME_LENGTH = 100
        const val MAX_LOCATION_LENGTH = 100
    }
}
