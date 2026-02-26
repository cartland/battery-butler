package com.chriscartland.batterybutler.presentationcore.components

import com.chriscartland.batterybutler.presentationcore.theme.IconColorRole
import kotlin.test.Test
import kotlin.test.assertEquals

class DeviceIconMapperTest {
    @Test
    fun allAvailableIconsReturnPrimary() {
        for (iconName in DeviceIconMapper.AvailableIcons) {
            assertEquals(
                IconColorRole.Primary,
                DeviceIconMapper.getIconColorRole(iconName),
                "Icon '$iconName' should use Primary color role",
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
}
