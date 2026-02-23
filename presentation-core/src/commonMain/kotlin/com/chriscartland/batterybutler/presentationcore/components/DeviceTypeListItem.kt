package com.chriscartland.batterybutler.presentationcore.components

import androidx.compose.foundation.layout.Column
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.tooling.preview.Preview
import com.chriscartland.batterybutler.domain.model.DeviceType
import com.chriscartland.batterybutler.presentationcore.theme.BatteryButlerTheme

@Composable
fun DeviceTypeListItem(
    deviceType: DeviceType,
    onClick: () -> Unit,
    modifier: Modifier = Modifier,
) {
    ButlerListItemCard(
        onClick = onClick,
        modifier = modifier,
        leading = {
            ButlerIconBox(
                icon = DeviceIconMapper.getIcon(deviceType.defaultIcon),
                contentDescription = "Device type icon",
            )
        },
        trailing = {
            Column(horizontalAlignment = Alignment.End) {
                Text(
                    text = "${deviceType.batteryQuantity}",
                    style = MaterialTheme.typography.titleLarge,
                    fontWeight = FontWeight.Bold,
                    color = MaterialTheme.colorScheme.onSurface,
                )
                Text(
                    text = deviceType.batteryType,
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                )
            }
        },
    ) {
        Text(
            text = deviceType.name,
            style = MaterialTheme.typography.titleMedium,
            fontWeight = FontWeight.SemiBold,
            maxLines = 1,
            overflow = TextOverflow.Ellipsis,
        )
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
