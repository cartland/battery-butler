package com.chriscartland.batterybutler.presentationcore.components

import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.BatteryFull
import androidx.compose.material3.Card
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import com.chriscartland.batterybutler.domain.model.Device
import com.chriscartland.batterybutler.domain.model.DeviceType
import com.chriscartland.batterybutler.presentationcore.theme.BatteryButlerTheme
import kotlin.time.Instant

@OptIn(kotlin.time.ExperimentalTime::class)
@Composable
fun DeviceListItem(
    device: Device,
    deviceType: DeviceType?,
    onClick: () -> Unit,
    modifier: Modifier = Modifier,
) {
    val daysSinceBatteryChange = remember(device.batteryLastReplaced) {
        if (device.batteryLastReplaced.toEpochMilliseconds() == 0L) {
            "N/A"
        } else {
            val now = Instant.parse("2026-01-18T17:00:00Z")
            "${now.minus(device.batteryLastReplaced).inWholeDays} days"
        }
    }

    Card(
        modifier = modifier
            .fillMaxWidth()
            .padding(horizontal = 16.dp, vertical = 6.dp)
            .clickable { onClick() },
        shape = RoundedCornerShape(12.dp),
    ) {
        Row(
            modifier = Modifier.padding(16.dp),
            verticalAlignment = Alignment.CenterVertically,
        ) {
            // Icon
            val iconName = deviceType?.defaultIcon
            Box(
                modifier = Modifier
                    .size(48.dp)
                    .clip(RoundedCornerShape(8.dp))
                    .background(DeviceIconMapper.getContainerColor(iconName)),
                contentAlignment = Alignment.Center,
            ) {
                Icon(
                    imageVector = DeviceIconMapper.getIcon(iconName),
                    contentDescription = deviceType?.name,
                    tint = DeviceIconMapper.getContentColor(iconName),
                    modifier = Modifier.size(24.dp),
                )
            }

            Spacer(modifier = Modifier.width(16.dp))

            // Name and Type/Location
            Column(modifier = Modifier.weight(1f)) {
                Text(
                    text = device.name,
                    style = MaterialTheme.typography.titleMedium,
                    fontWeight = FontWeight.SemiBold,
                    maxLines = 1,
                    overflow = TextOverflow.Ellipsis,
                )
                val typeName = deviceType?.name ?: "Unknown Type"
                val location = device.location
                val secondaryText = if (location.isNullOrBlank()) {
                    typeName
                } else {
                    "$typeName • $location"
                }

                Text(
                    text = secondaryText,
                    style = MaterialTheme.typography.bodyMedium,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                    maxLines = 1,
                    overflow = TextOverflow.Ellipsis,
                )
            }

            // Battery
            Column(
                horizontalAlignment = Alignment.CenterHorizontally,
                modifier = Modifier.width(60.dp),
            ) {
                Icon(
                    imageVector = Icons.Default.BatteryFull,
                    contentDescription = "Battery Status",
                    tint = MaterialTheme.colorScheme.onSurfaceVariant,
                    modifier = Modifier.size(24.dp),
                )
                Text(
                    text = daysSinceBatteryChange,
                    style = MaterialTheme.typography.labelSmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                )
            }
        }
    }
}

@Composable
fun DeviceTypeIconItem(
    iconName: String,
    isSelected: Boolean,
    onClick: () -> Unit,
) {
    Column(
        horizontalAlignment = Alignment.CenterHorizontally,
        verticalArrangement = androidx.compose.foundation.layout.Arrangement
            .spacedBy(4.dp),
        modifier = Modifier.clickable { onClick() },
    ) {
        Box(
            modifier = Modifier
                .size(64.dp)
                .clip(androidx.compose.foundation.shape.CircleShape)
                .background(if (isSelected) MaterialTheme.colorScheme.primary else MaterialTheme.colorScheme.surfaceVariant)
                .then(
                    if (isSelected) {
                        Modifier.border(
                            2.dp,
                            MaterialTheme.colorScheme.primary,
                            androidx.compose.foundation.shape.CircleShape,
                        )
                    } else {
                        Modifier
                    },
                ),
            contentAlignment = Alignment.Center,
        ) {
            Icon(
                imageVector = DeviceIconMapper.getIcon(iconName),
                contentDescription = null,
                tint = if (isSelected) MaterialTheme.colorScheme.onPrimary else MaterialTheme.colorScheme.onSurfaceVariant,
                modifier = Modifier.size(32.dp),
            )
        }
    }
}

@OptIn(kotlin.time.ExperimentalTime::class)
@Preview(showBackground = true)
@Composable
fun DeviceListItemPreview() {
    BatteryButlerTheme {
        val now = Instant.parse("2026-01-18T17:00:00Z")
        val device = Device("dev1", "Kitchen Smoke", "type1", now, now, "Kitchen")
        val type = DeviceType("type1", "Smoke Alarm", "detector_smoke")
        DeviceListItem(
            device = device,
            deviceType = type,
            onClick = {},
        )
    }
}

@Preview(showBackground = true)
@Composable
fun DeviceTypeIconItemPreview() {
    BatteryButlerTheme {
        DeviceTypeIconItem(
            iconName = "detector_smoke",
            isSelected = true,
            onClick = {},
        )
    }
}
