package com.chriscartland.batterybutler.domain.model

/**
 * Represents a category of battery-powered devices.
 *
 * Device types define shared characteristics for similar devices, such as
 * smoke alarms, thermostats, or remote controls. Each [Device] references
 * a DeviceType to inherit these defaults.
 *
 * @property id Unique identifier for this device type (UUID format).
 * @property name User-facing display name (e.g., "Smoke Alarm", "Thermostat").
 * @property defaultIcon Optional Material Icon name for display (e.g., "detector_smoke").
 *   See [DeviceIcons] for available icon mappings.
 * @property batteryType Default battery type for devices of this type (e.g., "AA", "9V").
 * @property batteryQuantity Number of batteries typically used by this device type.
 */
data class DeviceType(
    val id: String,
    val name: String,
    val defaultIcon: String? = null,
    val batteryType: String = "AA",
    val batteryQuantity: Int = 1,
)
