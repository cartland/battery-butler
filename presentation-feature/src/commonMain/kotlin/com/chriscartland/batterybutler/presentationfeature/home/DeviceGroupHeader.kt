package com.chriscartland.batterybutler.presentationfeature.home

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.semantics.clearAndSetSemantics
import androidx.compose.ui.semantics.heading
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.tooling.preview.Preview
import com.chriscartland.batterybutler.domain.model.Device
import com.chriscartland.batterybutler.domain.model.DeviceType
import com.chriscartland.batterybutler.presentationcore.components.ButlerIconBox
import com.chriscartland.batterybutler.presentationcore.components.ButlerIconBoxDefaults
import com.chriscartland.batterybutler.presentationcore.components.DeviceIconMapper
import com.chriscartland.batterybutler.presentationcore.components.batterySummary
import com.chriscartland.batterybutler.presentationcore.theme.BatteryButlerTheme
import com.chriscartland.batterybutler.presentationcore.theme.IconSize
import com.chriscartland.batterybutler.presentationcore.theme.Padding
import com.chriscartland.batterybutler.presentationmodel.home.GroupOption

/**
 * The device type a Home group header should describe, or null for a plain text header.
 *
 * Only grouping by type has one — a location group mixes types. The group key is the type's
 * *name*, so two types sharing a name land in one group; if their devices disagree on the type,
 * this returns null rather than showing one type's icon and battery as if it covered them all.
 */
fun groupHeaderDeviceType(
    groupOption: GroupOption,
    devices: List<Device>,
    deviceTypes: Map<String, DeviceType>,
): DeviceType? {
    if (groupOption != GroupOption.TYPE) return null
    val typeId = devices.map { it.typeId }.distinct().singleOrNull() ?: return null
    return deviceTypes[typeId]
}

/**
 * Sticky header for one group of the Home device list.
 *
 * With a [deviceType] (group-by-type) it reads `[icon] Smoke Alarm • 1 x 9V`: the type's icon,
 * because a device photo replaces the icon on its own row, and its battery so the replacement is
 * visible without opening a device. The name gives way (ellipsis) before the battery does — the
 * battery is the part you can't recover from anywhere else on screen.
 *
 * Without one it is the plain group name, as before.
 */
@Composable
fun DeviceGroupHeader(
    title: String,
    deviceType: DeviceType?,
    modifier: Modifier = Modifier,
) {
    Surface(
        modifier = modifier.fillMaxWidth(),
        color = MaterialTheme.colorScheme.surfaceVariant,
        contentColor = MaterialTheme.colorScheme.onSurfaceVariant,
    ) {
        Row(
            modifier = Modifier
                .padding(horizontal = Padding.standard, vertical = Padding.small)
                // One focus stop for screen readers: "Smoke Alarm, 1 x 9V", announced as a heading.
                .semantics(mergeDescendants = true) { heading() },
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.spacedBy(Padding.small),
        ) {
            if (deviceType != null) {
                val accent = DeviceIconMapper.getResolvedIconAccent(deviceType.defaultIcon)
                ButlerIconBox(
                    icon = DeviceIconMapper.getIcon(deviceType.defaultIcon),
                    // Decorative: the type name sits right next to it.
                    contentDescription = null,
                    containerColor = accent.container,
                    contentColor = accent.content,
                    size = ButlerIconBoxDefaults.CompactSize,
                    iconSize = IconSize.ExtraSmall,
                )
            }
            Text(
                text = title,
                modifier = Modifier.weight(1f, fill = false),
                style = MaterialTheme.typography.labelLarge,
                fontWeight = FontWeight.Bold,
                maxLines = 1,
                overflow = TextOverflow.Ellipsis,
            )
            if (deviceType != null) {
                Text(
                    text = "•", // @StringResourceExempt — a separator glyph, not translatable text
                    modifier = Modifier.clearAndSetSemantics {},
                    style = MaterialTheme.typography.labelLarge,
                )
                Text(
                    text = deviceType.batterySummary(),
                    style = MaterialTheme.typography.labelLarge,
                    maxLines = 1,
                )
            }
        }
    }
}

@Preview(showBackground = true)
@Composable
fun DeviceGroupHeaderPreview() {
    BatteryButlerTheme {
        Column {
            DeviceGroupHeader(
                title = "Smoke Alarm",
                deviceType = DeviceType("type1", "Smoke Alarm", "detector_smoke", batteryType = "9V"),
            )
            DeviceGroupHeader(
                title = "Smart Motion Sensor With An Unreasonably Long Name",
                deviceType = DeviceType(
                    "type2",
                    "Smart Motion Sensor With An Unreasonably Long Name",
                    "sensors",
                    batteryType = "CR2032",
                    batteryQuantity = 2,
                ),
            )
            DeviceGroupHeader(title = "Kitchen", deviceType = null)
        }
    }
}
