package com.chriscartland.batterybutler.domain.model

/**
 * Template for a common device type that can be preloaded on-device.
 * Unlike [DeviceType], templates don't have an ID — one is generated when added.
 */
data class CommonDeviceTypeTemplate(
    val name: String,
    val batteryType: String,
    val batteryQuantity: Int,
    val defaultIcon: String,
)

/**
 * Curated list of common battery-powered device types.
 * Used by PreloadCommonTypesUseCase to seed the database on user request.
 */
object CommonDeviceTypes {
    val types: List<CommonDeviceTypeTemplate> = listOf(
        CommonDeviceTypeTemplate("Smart Button", "CR2450", 1, "smart_button"),
        CommonDeviceTypeTemplate("Smart Motion Sensor", "CR2477", 1, "sensors"),
        CommonDeviceTypeTemplate("Tile Mate", "CR1632", 1, "location_on"),
        CommonDeviceTypeTemplate("Tile Pro", "CR2032", 1, "location_on"),
        CommonDeviceTypeTemplate("Calipers", "LR44", 1, "straighten"),
        CommonDeviceTypeTemplate("ULTRALOQ U-Bolt Pro Lock", "AA", 4, "lock"),
        CommonDeviceTypeTemplate("Digital Angle Ruler", "CR2032", 1, "straighten"),
        CommonDeviceTypeTemplate("Tile Wallet", "Thin Tile", 1, "account_balance_wallet"),
        CommonDeviceTypeTemplate("1-9V Smoke Detector", "9V", 1, "detector_smoke"),
        CommonDeviceTypeTemplate("2-AA Smoke Detector", "AA", 2, "detector_smoke"),
        CommonDeviceTypeTemplate("Orbit 57896 Sprinkler Timer", "CR2032", 1, "water_drop"),
    )
}
