package com.chriscartland.batterybutler.domain.model

import kotlin.time.Instant

/**
 * Represents a battery replacement event for a device.
 *
 * Battery events form an immutable history of when batteries were replaced in a device.
 * The most recent event's [date] should match the associated [Device.batteryLastReplaced].
 *
 * @property id Unique identifier for this event (UUID format).
 * @property deviceId Reference to the [Device] this event belongs to.
 * @property date Timestamp when the battery was replaced.
 * @property batteryType Optional battery type used (e.g., "AA", "AAA", "CR2032").
 *   If null, defaults to the device type's standard battery.
 * @property notes Optional user-provided notes about this replacement.
 */
@OptIn(kotlin.time.ExperimentalTime::class)
data class BatteryEvent(
    val id: String,
    val deviceId: String,
    val date: Instant,
    val batteryType: String? = null,
    val notes: String? = null,
)
