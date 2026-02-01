package com.chriscartland.batterybutler.testcommon

import com.chriscartland.batterybutler.domain.model.BatteryEvent
import com.chriscartland.batterybutler.domain.model.Device
import com.chriscartland.batterybutler.domain.model.DeviceType
import kotlin.time.ExperimentalTime
import kotlin.time.Instant

/**
 * Helper object for creating test data with sensible defaults.
 * Use these builders to avoid duplicating test data construction across tests.
 *
 * Example usage:
 * ```kotlin
 * val device = TestDevices.createDevice(name = "My Device")
 * val deviceType = TestDevices.createDeviceType(batteryType = "AAA")
 * val event = TestDevices.createBatteryEvent(deviceId = device.id)
 * ```
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

    fun createDeviceType(
        id: String = "test-type-id",
        name: String = "Test Device Type",
        defaultIcon: String? = "default",
        batteryType: String = "AA",
        batteryQuantity: Int = 2,
    ): DeviceType =
        DeviceType(
            id = id,
            name = name,
            defaultIcon = defaultIcon,
            batteryType = batteryType,
            batteryQuantity = batteryQuantity,
        )

    fun createBatteryEvent(
        id: String = "test-event-id",
        deviceId: String = "test-device-id",
        date: Instant = Instant.DISTANT_PAST,
        batteryType: String? = "AA",
        notes: String? = null,
    ): BatteryEvent =
        BatteryEvent(
            id = id,
            deviceId = deviceId,
            date = date,
            batteryType = batteryType,
            notes = notes,
        )
}
