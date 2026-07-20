package com.chriscartland.batterybutler.domain.model

import kotlin.time.Instant

/**
 * Represents a device with a battery that needs periodic replacement.
 *
 * This is the core domain entity for tracking battery-powered devices. Each device
 * belongs to a [DeviceType] which defines common characteristics like battery type.
 *
 * @property id Unique identifier for the device (UUID format).
 * @property name User-facing display name for the device (e.g., "Kitchen Smoke Alarm").
 * @property typeId Reference to the [DeviceType] this device belongs to.
 * @property batteryLastReplaced Timestamp when the battery was last replaced.
 * @property lastUpdated Timestamp when any device metadata was last modified.
 * @property location Optional user-defined location (e.g., "Kitchen", "Garage").
 * @property imagePath Optional path to a user-provided image of the device. Client-owned and
 *   opaque to the server; do not confuse with [imageEtag].
 * @property imageEtag Opaque content hash of the device's server-stored photo, or null if it has
 *   none. Server-managed and read-only for the client -- it is the cache key and change signal
 *   for the Labs backend's image endpoints (`PUT/GET/DELETE .../devices/{id}/image`), populated
 *   only from a sync snapshot, never sent on a push. See `docs/DEVICE_IMAGES.md`.
 * @throws IllegalArgumentException if [id] or [name] is blank.
 */
@OptIn(kotlin.time.ExperimentalTime::class)
data class Device(
    val id: String,
    val name: String,
    val typeId: String,
    val batteryLastReplaced: Instant,
    val lastUpdated: Instant,
    val location: String? = null,
    val imagePath: String? = null,
    val imageEtag: String? = null,
) {
    init {
        require(id.isNotBlank()) { "Device ID cannot be blank" }
        require(name.isNotBlank()) { "Device name cannot be blank" }
    }
}
