package com.chriscartland.batterybutler.usecase

import com.chriscartland.batterybutler.domain.model.Device
import kotlin.time.ExperimentalTime
import kotlin.time.Instant

/**
 * Helper object for creating test devices with sensible defaults.
 */
@OptIn(ExperimentalTime::class)
object TestDevices {
    fun createDevice(
        id: String = "test-device-id",
        name: String = "Test Device",
        typeId: String = "type-1",
        batteryLastReplaced: Instant = Instant.DISTANT_PAST,
        lastUpdated: Instant = Instant.DISTANT_PAST,
        location: String? = null,
        imagePath: String? = null,
    ): Device =
        Device(
            id = id,
            name = name,
            typeId = typeId,
            batteryLastReplaced = batteryLastReplaced,
            lastUpdated = lastUpdated,
            location = location,
            imagePath = imagePath,
        )
}
