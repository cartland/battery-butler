package com.chriscartland.batterybutler.presentationcore.components

import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.tooling.preview.Preview
import com.chriscartland.batterybutler.composeresources.composeStringResource
import com.chriscartland.batterybutler.composeresources.generated.resources.Res
import com.chriscartland.batterybutler.composeresources.generated.resources.content_desc_device_type_icon
import com.chriscartland.batterybutler.domain.model.DeviceType
import com.chriscartland.batterybutler.presentationcore.theme.BatteryButlerTheme
import com.chriscartland.batterybutler.presentationcore.theme.IconSize
import com.chriscartland.batterybutler.presentationcore.theme.Padding
import com.chriscartland.batterybutler.presentationmodel.home.DensityOption

/**
 * A device type row.
 *
 * Honours the same app-wide density as the Home device list:
 * - [DensityOption.EXPANDED] (default): 48.dp icon, name, and a "N x BatteryType" secondary line.
 * - [DensityOption.COMPACT]: 24.dp icon and the battery summary moved to the trailing slot, so the
 *   card is only as tall as its single line of text.
 *
 * The battery summary moves rather than disappearing — unlike the device row's "type • location"
 * secondary line, it is the only thing distinguishing two types with similar names, so dropping it
 * would make the compact list ambiguous.
 */
@Composable
fun DeviceTypeListItem(
    deviceType: DeviceType,
    onClick: () -> Unit,
    modifier: Modifier = Modifier,
    density: DensityOption = DensityOption.EXPANDED,
) {
    val isCompact = density == DensityOption.COMPACT
    val leadingSize = if (isCompact) ButlerIconBoxDefaults.CompactSize else ButlerIconBoxDefaults.Size
    val batterySummary = "${deviceType.batteryQuantity} x ${deviceType.batteryType}"

    ButlerListItemCard(
        onClick = onClick,
        modifier = modifier,
        contentPadding = if (isCompact) {
            PaddingValues(horizontal = Padding.standard, vertical = Padding.small)
        } else {
            PaddingValues(Padding.standard)
        },
        leadingSpacing = if (isCompact) Padding.medium else Padding.standard,
        leading = {
            val accent = DeviceIconMapper.getResolvedIconAccent(deviceType.defaultIcon)
            ButlerIconBox(
                icon = DeviceIconMapper.getIcon(deviceType.defaultIcon),
                contentDescription = composeStringResource(Res.string.content_desc_device_type_icon),
                containerColor = accent.container,
                contentColor = accent.content,
                size = leadingSize,
                iconSize = if (isCompact) IconSize.ExtraSmall else IconSize.Medium,
            )
        },
        trailing = if (isCompact) {
            {
                Text(
                    text = batterySummary,
                    style = MaterialTheme.typography.labelLarge,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                    maxLines = 1,
                    overflow = TextOverflow.Ellipsis,
                )
            }
        } else {
            null
        },
    ) {
        Text(
            text = deviceType.name,
            style = MaterialTheme.typography.titleMedium,
            fontWeight = FontWeight.SemiBold,
            maxLines = 1,
            overflow = TextOverflow.Ellipsis,
        )
        if (!isCompact) {
            Text(
                text = batterySummary,
                style = MaterialTheme.typography.bodyMedium,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
                maxLines = 1,
                overflow = TextOverflow.Ellipsis,
            )
        }
    }
}

@Preview(showBackground = true)
@Composable
fun DeviceTypeListItemPreview() {
    BatteryButlerTheme {
        DeviceTypeListItem(
            deviceType = DeviceType("type1", "Smoke Alarm", "detector_smoke"),
            onClick = {},
        )
    }
}
