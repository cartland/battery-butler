package com.chriscartland.batterybutler.domain.demo

import com.benasher44.uuid.uuid4
import com.chriscartland.batterybutler.domain.model.DeviceType

object DemoData {
    fun getDefaultDeviceTypes(existingTypes: Map<String, DeviceType> = emptyMap()): List<DeviceType> =
        listOf(
            DeviceType(
                id = existingTypes["Smart Button"]?.id ?: uuid4().toString(),
                name = "Smart Button",
                batteryType = "CR2450", // Assumption based on common smart buttons (e.g. Zigbee)
                batteryQuantity = 1,
                defaultIcon = "smart_button",
            ),
            DeviceType(
                id = existingTypes["Smart Motion Sensor"]?.id ?: uuid4().toString(),
                name = "Smart Motion Sensor",
                batteryType = "CR2477", // Assumption for some sensors, or CR123A
                batteryQuantity = 1,
                defaultIcon = "sensors",
            ),
            DeviceType(
                id = existingTypes["Tile Mate"]?.id ?: uuid4().toString(),
                name = "Tile Mate",
                batteryType = "CR1632",
                batteryQuantity = 1,
                defaultIcon = "location_on",
            ),
            DeviceType(
                id = existingTypes["Tile Pro"]?.id ?: uuid4().toString(),
                name = "Tile Pro",
                batteryType = "CR2032",
                batteryQuantity = 1,
                defaultIcon = "location_on",
            ),
            DeviceType(
                id = existingTypes["Calipers"]?.id ?: uuid4().toString(),
                name = "Calipers",
                batteryType = "LR44",
                batteryQuantity = 1,
                defaultIcon = "straighten",
            ),
            DeviceType(
                id = existingTypes["ULTRALOQ U-Bolt Pro Lock"]?.id ?: uuid4().toString(),
                name = "ULTRALOQ U-Bolt Pro Lock",
                batteryType = "AA",
                batteryQuantity = 4,
                defaultIcon = "lock",
            ),
            DeviceType(
                id = existingTypes["Digital Angle Ruler"]?.id ?: uuid4().toString(),
                name = "Digital Angle Ruler",
                batteryType = "CR2032",
                batteryQuantity = 1,
                defaultIcon = "straighten",
            ),
            DeviceType(
                id = existingTypes["Tile Wallet"]?.id ?: uuid4().toString(),
                name = "Tile Wallet",
                batteryType = "Thin Tile",
                batteryQuantity = 1,
                defaultIcon = "account_balance_wallet",
            ),
            DeviceType(
                id = existingTypes["1-9V Smoke Detector"]?.id ?: uuid4().toString(),
                name = "1-9V Smoke Detector",
                batteryType = "9V",
                batteryQuantity = 1,
                defaultIcon = "detector_smoke",
            ),
            DeviceType(
                id = existingTypes["2-AA Smoke Detector"]?.id ?: uuid4().toString(),
                name = "2-AA Smoke Detector",
                batteryType = "AA",
                batteryQuantity = 2,
                defaultIcon = "detector_smoke",
            ),
            DeviceType(
                id = existingTypes["Orbit 57896 Sprinkler Timer"]?.id ?: uuid4().toString(),
                name = "Orbit 57896 Sprinkler Timer",
                batteryType = "CR2032", // User didn't specify but sticking to previous assumption or generic
                batteryQuantity = 1,
                defaultIcon = "water_drop",
            ),
        )
}
