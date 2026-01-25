package com.chriscartland.batterybutler.fixtures

import com.chriscartland.batterybutler.domain.model.BatteryEvent
import com.chriscartland.batterybutler.domain.model.Device
import com.chriscartland.batterybutler.domain.model.DeviceType
import kotlin.time.Instant

@OptIn(kotlin.time.ExperimentalTime::class)
object DemoData {
    fun getDefaultDeviceTypes(existingTypes: Map<String, DeviceType> = emptyMap()): List<DeviceType> =
        listOf(
            DeviceType(
                id = existingTypes["Smart Button"]?.id ?: "type-smart-button",
                name = "Smart Button",
                batteryType = "CR2450", // Assumption based on common smart buttons (e.g. Zigbee)
                batteryQuantity = 1,
                defaultIcon = "smart_button",
            ),
            DeviceType(
                id = existingTypes["Smart Motion Sensor"]?.id ?: "type-smart-motion-sensor",
                name = "Smart Motion Sensor",
                batteryType = "CR2477", // Assumption for some sensors, or CR123A
                batteryQuantity = 1,
                defaultIcon = "sensors",
            ),
            DeviceType(
                id = existingTypes["Tile Mate"]?.id ?: "type-tile-mate",
                name = "Tile Mate",
                batteryType = "CR1632",
                batteryQuantity = 1,
                defaultIcon = "location_on",
            ),
            DeviceType(
                id = existingTypes["Tile Pro"]?.id ?: "type-tile-pro",
                name = "Tile Pro",
                batteryType = "CR2032",
                batteryQuantity = 1,
                defaultIcon = "location_on",
            ),
            DeviceType(
                id = existingTypes["Calipers"]?.id ?: "type-calipers",
                name = "Calipers",
                batteryType = "LR44",
                batteryQuantity = 1,
                defaultIcon = "straighten",
            ),
            DeviceType(
                id = existingTypes["ULTRALOQ U-Bolt Pro Lock"]?.id ?: "type-ultraloq-u-bolt-pro-lock",
                name = "ULTRALOQ U-Bolt Pro Lock",
                batteryType = "AA",
                batteryQuantity = 4,
                defaultIcon = "lock",
            ),
            DeviceType(
                id = existingTypes["Digital Angle Ruler"]?.id ?: "type-digital-angle-ruler",
                name = "Digital Angle Ruler",
                batteryType = "CR2032",
                batteryQuantity = 1,
                defaultIcon = "straighten",
            ),
            DeviceType(
                id = existingTypes["Tile Wallet"]?.id ?: "type-tile-wallet",
                name = "Tile Wallet",
                batteryType = "Thin Tile",
                batteryQuantity = 1,
                defaultIcon = "account_balance_wallet",
            ),
            DeviceType(
                id = existingTypes["1-9V Smoke Detector"]?.id ?: "type-1-9v-smoke-detector",
                name = "1-9V Smoke Detector",
                batteryType = "9V",
                batteryQuantity = 1,
                defaultIcon = "detector_smoke",
            ),
            DeviceType(
                id = existingTypes["2-AA Smoke Detector"]?.id ?: "type-2-aa-smoke-detector",
                name = "2-AA Smoke Detector",
                batteryType = "AA",
                batteryQuantity = 2,
                defaultIcon = "detector_smoke",
            ),
            DeviceType(
                id = existingTypes["Orbit 57896 Sprinkler Timer"]?.id ?: "type-orbit-57896-sprinkler-timer",
                name = "Orbit 57896 Sprinkler Timer",
                batteryType = "CR2032", // User didn't specify but sticking to previous assumption or generic
                batteryQuantity = 1,
                defaultIcon = "water_drop",
            ),
        )

    fun getDefaultDevices(deviceTypes: List<DeviceType>): List<Device> {
        val typeMap = deviceTypes.associateBy { it.name }
        val now = Instant.fromEpochMilliseconds(1767225600000) // 2026-01-01

        // Deterministic UUIDs for devices
        val devices = listOf(
            Device(
                id = "device-living-room-smoke",
                name = "Living Room Smoke Detector",
                typeId = typeMap["1-9V Smoke Detector"]?.id ?: return emptyList(),
                batteryLastReplaced = Instant.fromEpochMilliseconds(1704067200000), // 2024-01-01
                lastUpdated = now,
                location = "Living Room",
            ),
            Device(
                id = "device-kitchen-smoke",
                name = "Kitchen Smoke Detector",
                typeId = typeMap["2-AA Smoke Detector"]?.id ?: return emptyList(),
                batteryLastReplaced = Instant.fromEpochMilliseconds(1750032000000), // 2025-06-15
                lastUpdated = now,
                location = "Kitchen",
            ),
            Device(
                id = "device-front-door-lock",
                name = "Front Door Lock",
                typeId = typeMap["ULTRALOQ U-Bolt Pro Lock"]?.id ?: return emptyList(),
                batteryLastReplaced = Instant.fromEpochMilliseconds(1763683200000), // 2025-11-20
                lastUpdated = now,
                location = "Front Door",
            ),
            Device(
                id = "device-car-keys-tile",
                name = "Car Keys Tile",
                typeId = typeMap["Tile Mate"]?.id ?: return emptyList(),
                batteryLastReplaced = Instant.fromEpochMilliseconds(1736467200000), // 2025-01-10
                lastUpdated = now,
                location = "Car",
            ),
        )
        return devices
    }

    fun getDefaultEvents(devices: List<Device>): List<BatteryEvent> {
        val deviceMap = devices.associateBy { it.name }
        val events = mutableListOf<BatteryEvent>()

        // Living Room Smoke Detector Events
        deviceMap["Living Room Smoke Detector"]?.let { device ->
            events.add(
                BatteryEvent(
                    id = "${device.id}-event-1",
                    deviceId = device.id,
                    date = Instant.fromEpochMilliseconds(1672531200000), // 2023-01-01
                    batteryType = "9V",
                    notes = "Initial installation",
                ),
            )
            events.add(
                BatteryEvent(
                    id = "${device.id}-event-2",
                    deviceId = device.id,
                    date = Instant.fromEpochMilliseconds(1704067200000), // 2024-01-01
                    batteryType = "9V",
                    notes = "Yearly replacement",
                ),
            )
        }

        // Kitchen Smoke Detector Events
        deviceMap["Kitchen Smoke Detector"]?.let { device ->
            events.add(
                BatteryEvent(
                    id = "${device.id}-event-1",
                    deviceId = device.id,
                    date = Instant.fromEpochMilliseconds(1718409600000), // 2024-06-15
                    batteryType = "AA",
                    notes = "Initial installation",
                ),
            )
            events.add(
                BatteryEvent(
                    id = "${device.id}-event-2",
                    deviceId = device.id,
                    date = Instant.fromEpochMilliseconds(1750032000000), // 2025-06-15
                    batteryType = "AA",
                    notes = "Replaced with Duracell",
                ),
            )
        }

        // Front Door Lock Events
        deviceMap["Front Door Lock"]?.let { device ->
            events.add(
                BatteryEvent(
                    id = "${device.id}-event-1",
                    deviceId = device.id,
                    date = Instant.fromEpochMilliseconds(1763683200000), // 2025-11-20
                    batteryType = "AA",
                    notes = "New batteries installed",
                ),
            )
        }

        // Car Keys Tile
        deviceMap["Car Keys Tile"]?.let { device ->
            events.add(
                BatteryEvent(
                    id = "${device.id}-event-1",
                    deviceId = device.id,
                    date = Instant.fromEpochMilliseconds(1736467200000), // 2025-01-10
                    batteryType = "CR1632",
                    notes = "Battery low warning",
                ),
            )
        }

        return events
    }
}
