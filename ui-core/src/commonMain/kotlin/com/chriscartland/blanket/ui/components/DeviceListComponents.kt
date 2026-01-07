package com.chriscartland.blanket.ui.components

import androidx.compose.foundation.background
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
import androidx.compose.material.icons.filled.BatteryAlert
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
import androidx.compose.ui.unit.dp
import com.chriscartland.blanket.domain.model.Device
import com.chriscartland.blanket.domain.model.DeviceType
import kotlinx.datetime.Clock

@Composable
fun DeviceListItem(
    device: Device,
    deviceType: DeviceType?,
    onClick: () -> Unit,
    modifier: Modifier = Modifier,
) {
    val daysSinceBatteryChange = remember(device.batteryLastReplaced) {
        val now = Clock.System.now()
        now.minus(device.batteryLastReplaced).inWholeDays
    }

    Card(
        modifier = modifier
            .fillMaxWidth()
            .padding(horizontal = 16.dp, vertical = 6.dp)
            .clickable { onClick() },
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
                    .clip(RoundedCornerShape(12.dp)) // Design calls for rounded-xl which is roughly 12dp
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

            // Name and Type
            Column(modifier = Modifier.weight(1f)) {
                Text(
                    text = device.name,
                    style = MaterialTheme.typography.titleMedium,
                    fontWeight = FontWeight.SemiBold,
                )
                Text(
                    text = deviceType?.name ?: "Unknown Type",
                    style = MaterialTheme.typography.bodyMedium,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
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
                    text = "$daysSinceBatteryChange days",
                    style = MaterialTheme.typography.labelSmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                )
            }
        }
    }
}
