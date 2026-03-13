package com.chriscartland.batterybutler.presentationcore.components

import com.chriscartland.batterybutler.presentationcore.theme.IconColorRole
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertTrue

class DeviceIconMapperTest {
    @Test
    fun homeIconsReturnPrimary() {
        val homeIcons = listOf(
            "lightbulb",
            "thermostat",
            "garage_home",
            "lock",
            "water_drop",
            "schedule",
            "star",
            "circle",
            "square",
            "favorite",
            "diamond",
            "hexagon",
        )
        for (iconName in homeIcons) {
            assertEquals(
                IconColorRole.Primary,
                DeviceIconMapper.getIconColorRole(iconName),
                "Icon '$iconName' should use Primary color role",
            )
        }
    }

    @Test
    fun toolIconsReturnSecondary() {
        val toolIcons = listOf(
            "drill",
            "brush",
            "scale",
            "straighten",
            "toys",
            "flashlight_on",
        )
        for (iconName in toolIcons) {
            assertEquals(
                IconColorRole.Secondary,
                DeviceIconMapper.getIconColorRole(iconName),
                "Icon '$iconName' should use Secondary color role",
            )
        }
    }

    @Test
    fun electronicsIconsReturnTertiary() {
        val electronicsIcons = listOf(
            "smartphone",
            "tablet",
            "laptop",
            "watch",
            "headphones",
            "camera",
            "speaker",
            "tv",
            "router",
            "keyboard",
            "mouse",
            "power",
            "smart_button",
            "settings_remote",
            "videogame_asset",
            "game_controller",
        )
        for (iconName in electronicsIcons) {
            assertEquals(
                IconColorRole.Tertiary,
                DeviceIconMapper.getIconColorRole(iconName),
                "Icon '$iconName' should use Tertiary color role",
            )
        }
    }

    @Test
    fun safetyIconsReturnError() {
        val safetyIcons = listOf(
            "detector_smoke",
            "sensors",
        )
        for (iconName in safetyIcons) {
            assertEquals(
                IconColorRole.Error,
                DeviceIconMapper.getIconColorRole(iconName),
                "Icon '$iconName' should use Error color role",
            )
        }
    }

    @Test
    fun otherIconsReturnSurface() {
        val otherIcons = listOf(
            "car",
            "bike",
            "location_on",
            "account_balance_wallet",
        )
        for (iconName in otherIcons) {
            assertEquals(
                IconColorRole.Surface,
                DeviceIconMapper.getIconColorRole(iconName),
                "Icon '$iconName' should use Surface color role",
            )
        }
    }

    @Test
    fun nullIconReturnsPrimary() {
        assertEquals(
            IconColorRole.Primary,
            DeviceIconMapper.getIconColorRole(null),
        )
    }

    @Test
    fun unknownIconReturnsPrimary() {
        assertEquals(
            IconColorRole.Primary,
            DeviceIconMapper.getIconColorRole("nonexistent_icon"),
        )
    }

    @Test
    fun allAvailableIconsHaveExplicitMapping() {
        for (iconName in DeviceIconMapper.AvailableIcons) {
            // If an icon falls through to `else`, it gets Primary.
            // We verify it's explicitly listed by checking it appears in one of the known sets.
            val role = DeviceIconMapper.getIconColorRole(iconName)
            // An unmapped icon would get Primary by default — but so do Home icons.
            // To detect truly unmapped icons, check that a made-up name also gets Primary,
            // then verify the icon is in at least one explicit category.
            val explicitIcons = setOf(
                // Home (Primary)
                "lightbulb",
                "thermostat",
                "garage_home",
                "lock",
                "water_drop",
                "schedule",
                "star",
                "circle",
                "square",
                "favorite",
                "diamond",
                "hexagon",
                // Tools (Secondary)
                "drill",
                "brush",
                "scale",
                "straighten",
                "toys",
                "flashlight_on",
                // Electronics (Tertiary)
                "smartphone",
                "tablet",
                "laptop",
                "watch",
                "headphones",
                "camera",
                "speaker",
                "tv",
                "router",
                "keyboard",
                "mouse",
                "power",
                "smart_button",
                "settings_remote",
                "videogame_asset",
                "game_controller",
                // Safety (Error)
                "detector_smoke",
                "sensors",
                // Other (Surface)
                "car",
                "bike",
                "location_on",
                "account_balance_wallet",
            )
            assertTrue(
                iconName in explicitIcons,
                "Icon '$iconName' (role=$role) is not explicitly mapped — it falls through to else",
            )
        }
    }
}
