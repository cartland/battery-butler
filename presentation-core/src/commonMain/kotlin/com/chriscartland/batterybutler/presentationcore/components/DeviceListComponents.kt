package com.chriscartland.batterybutler.presentationcore.components

import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.BatteryFull
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import com.chriscartland.batterybutler.domain.model.Device
import com.chriscartland.batterybutler.domain.model.DeviceType
import com.chriscartland.batterybutler.presentationcore.theme.BatteryButlerTheme
import com.chriscartland.batterybutler.presentationcore.theme.IconSize
import com.chriscartland.batterybutler.presentationcore.theme.LocalButlerColors
import kotlinx.datetime.TimeZone
import kotlinx.datetime.daysUntil
import kotlinx.datetime.toLocalDateTime
import kotlin.time.Clock
import kotlin.time.Instant

/**
 * Returns a color based on battery age in days.
 * - 0-179 days: default gray (normal)
 * - 180-364 days: dark amber (warning, WCAG AA contrast >= 4.5:1)
 * - 365+ days: error red (danger)
 * - null (no replacement date): default gray
 */
@Composable
private fun batteryAgeColor(days: Int?): Color {
    if (days == null) return MaterialTheme.colorScheme.onSurfaceVariant
    return when {
        days >= 365 -> MaterialTheme.colorScheme.error
        days >= 180 -> LocalButlerColors.current.batteryWarning
        else -> MaterialTheme.colorScheme.onSurfaceVariant
    }
}

@OptIn(kotlin.time.ExperimentalTime::class)
@Composable
fun DeviceListItem(
    device: Device,
    deviceType: DeviceType?,
    onClick: () -> Unit,
    modifier: Modifier = Modifier,
    nowInstant: Instant = Clock.System.now(),
) {
    val daysInt = remember(device.batteryLastReplaced, nowInstant) {
        if (device.batteryLastReplaced.toEpochMilliseconds() == 0L) {
            null
        } else {
            val timeZone = TimeZone.currentSystemDefault()
            val now = nowInstant.toLocalDateTime(timeZone).date
            val eventDate = device.batteryLastReplaced.toLocalDateTime(timeZone).date
            eventDate.daysUntil(now)
        }
    }

    val daysSinceBatteryChange = when {
        daysInt == null -> "N/A"
        daysInt == 1 -> "1 day"
        else -> "$daysInt days"
    }

    val ageColor = batteryAgeColor(daysInt)
    val ageFontWeight = if (daysInt != null && daysInt >= 365) FontWeight.Bold else null

    ButlerListItemCard(
        onClick = onClick,
        modifier = modifier,
        leading = {
            val accent = DeviceIconMapper.getResolvedIconAccent(deviceType?.defaultIcon)
            ButlerIconBox(
                icon = DeviceIconMapper.getIcon(deviceType?.defaultIcon),
                contentDescription = deviceType?.name ?: "Device Icon",
                containerColor = accent.container,
                contentColor = accent.content,
            )
        },
        trailing = {
            Column(
                horizontalAlignment = Alignment.CenterHorizontally,
                modifier = Modifier.width(60.dp),
            ) {
                Icon(
                    imageVector = Icons.Default.BatteryFull,
                    contentDescription = "Battery Age",
                    tint = ageColor,
                    modifier = Modifier.size(IconSize.Medium),
                )
                Text(
                    text = daysSinceBatteryChange,
                    style = MaterialTheme.typography.labelSmall,
                    color = ageColor,
                    fontWeight = ageFontWeight,
                )
            }
        },
    ) {
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
}

@Composable
fun DeviceTypeIconItem(
    iconName: String,
    isSelected: Boolean,
    onClick: () -> Unit,
    modifier: Modifier = Modifier,
) {
    Column(
        horizontalAlignment = Alignment.CenterHorizontally,
        verticalArrangement = androidx.compose.foundation.layout.Arrangement
            .spacedBy(4.dp),
        modifier = modifier.clickable { onClick() },
    ) {
        Box(
            modifier = Modifier
                .size(64.dp)
                .clip(androidx.compose.foundation.shape.CircleShape)
                .background(if (isSelected) MaterialTheme.colorScheme.primary else MaterialTheme.colorScheme.primaryContainer)
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
                contentDescription = if (isSelected) {
                    "Selected: ${iconName.replace("_", " ")}"
                } else {
                    iconName.replace("_", " ")
                },
                tint = if (isSelected) MaterialTheme.colorScheme.onPrimary else MaterialTheme.colorScheme.onPrimaryContainer,
                modifier = Modifier.size(IconSize.Large),
            )
        }
    }
}

@OptIn(kotlin.time.ExperimentalTime::class)
@Preview(showBackground = true)
@Composable
fun DeviceListItemPreview() {
    BatteryButlerTheme {
        // Use fixed dates for stable screenshots
        val nowInstant = Instant.parse("2026-01-18T17:00:00Z")
        val batteryReplacedInstant = Instant.parse("2026-01-13T17:00:00Z") // 5 days ago
        val device = Device("dev1", "Kitchen Smoke", "type1", batteryReplacedInstant, nowInstant, "Kitchen")
        val type = DeviceType("type1", "Smoke Alarm", "detector_smoke")
        DeviceListItem(
            device = device,
            deviceType = type,
            onClick = {},
            nowInstant = nowInstant,
        )
    }
}

@OptIn(kotlin.time.ExperimentalTime::class)
@Composable
fun DeviceListItemRecentPreview() {
    BatteryButlerTheme {
        val nowInstant = Instant.parse("2026-01-18T17:00:00Z")
        val batteryReplacedInstant = Instant.parse("2026-01-13T17:00:00Z") // 5 days ago
        val device = Device("dev1", "Kitchen Smoke", "type1", batteryReplacedInstant, nowInstant, "Kitchen")
        val type = DeviceType("type1", "Smoke Alarm", "detector_smoke")
        DeviceListItem(
            device = device,
            deviceType = type,
            onClick = {},
            nowInstant = nowInstant,
        )
    }
}

@OptIn(kotlin.time.ExperimentalTime::class)
@Preview(showBackground = true)
@Composable
fun DeviceListItemOldPreview() {
    BatteryButlerTheme {
        val nowInstant = Instant.parse("2026-01-18T17:00:00Z")
        val batteryReplacedInstant = Instant.parse("2025-07-02T17:00:00Z") // 200 days ago
        val device = Device("dev2", "Hallway CO Detector", "type2", batteryReplacedInstant, nowInstant, "Hallway")
        val type = DeviceType("type2", "CO Detector", "detector_co")
        DeviceListItem(
            device = device,
            deviceType = type,
            onClick = {},
            nowInstant = nowInstant,
        )
    }
}

@OptIn(kotlin.time.ExperimentalTime::class)
@Preview(showBackground = true)
@Composable
fun DeviceListItemVeryOldPreview() {
    BatteryButlerTheme {
        val nowInstant = Instant.parse("2026-01-18T17:00:00Z")
        val batteryReplacedInstant = Instant.parse("2024-12-02T17:00:00Z") // 412 days ago
        val device = Device("dev3", "Bedroom Smoke", "type1", batteryReplacedInstant, nowInstant, "Bedroom")
        val type = DeviceType("type1", "Smoke Alarm", "detector_smoke")
        DeviceListItem(
            device = device,
            deviceType = type,
            onClick = {},
            nowInstant = nowInstant,
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
