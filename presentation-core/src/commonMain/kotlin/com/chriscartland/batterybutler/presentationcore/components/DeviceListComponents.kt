package com.chriscartland.batterybutler.presentationcore.components

import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.layout.widthIn
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import com.chriscartland.batterybutler.composeresources.composeStringResource
import com.chriscartland.batterybutler.composeresources.generated.resources.Res
import com.chriscartland.batterybutler.composeresources.generated.resources.content_desc_device_photo
import com.chriscartland.batterybutler.domain.model.Device
import com.chriscartland.batterybutler.domain.model.DeviceImageBytes
import com.chriscartland.batterybutler.domain.model.DeviceType
import com.chriscartland.batterybutler.presentationcore.theme.BatteryButlerTheme
import com.chriscartland.batterybutler.presentationcore.theme.IconSize
import com.chriscartland.batterybutler.presentationcore.theme.LocalButlerColors
import com.chriscartland.batterybutler.presentationcore.theme.Padding
import com.chriscartland.batterybutler.presentationmodel.home.DensityOption
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

/** Single-line battery age for a compact row, e.g. "5 days" / "1 day" / "—". */
private fun compactAgeLabel(days: Int?): String =
    when {
        days == null -> "—"
        days == 1 -> "$days day"
        else -> "$days days"
    }

/**
 * A device row in the Home list.
 *
 * [density] controls how much vertical room the row takes:
 * - [DensityOption.EXPANDED] (default): a 48.dp icon/photo, the device name, and a
 *   "type • location" secondary line, with the battery age stacked in the trailing slot.
 * - [DensityOption.COMPACT]: name only, battery age on a single trailing line, and a 24.dp
 *   icon/photo so nothing in the row is taller than the text itself.
 */
@OptIn(kotlin.time.ExperimentalTime::class)
@Composable
fun DeviceListItem(
    device: Device,
    deviceType: DeviceType?,
    onClick: () -> Unit,
    modifier: Modifier = Modifier,
    nowInstant: Instant = Clock.System.now(),
    imageBytes: DeviceImageBytes? = null,
    density: DensityOption = DensityOption.EXPANDED,
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

    val ageColor = batteryAgeColor(daysInt)
    val isCompact = density == DensityOption.COMPACT
    val leadingSize = if (isCompact) ButlerIconBoxDefaults.CompactSize else ButlerIconBoxDefaults.Size

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
            val imageBitmap = rememberDeviceImageBitmap(device.imageEtag, imageBytes)
            if (imageBitmap != null) {
                Image(
                    bitmap = imageBitmap,
                    contentDescription = composeStringResource(Res.string.content_desc_device_photo),
                    contentScale = ContentScale.Crop,
                    modifier = Modifier.size(leadingSize).clip(MaterialTheme.shapes.small),
                )
            } else {
                val accent = DeviceIconMapper.getResolvedIconAccent(deviceType?.defaultIcon)
                ButlerIconBox(
                    icon = DeviceIconMapper.getIcon(deviceType?.defaultIcon),
                    contentDescription = deviceType?.name ?: "Device Icon",
                    containerColor = accent.container,
                    contentColor = accent.content,
                    size = leadingSize,
                    iconSize = if (isCompact) IconSize.ExtraSmall else IconSize.Medium,
                )
            }
        },
        trailing = {
            if (isCompact) {
                // widthIn (not width) so a four-digit age still right-aligns without truncating.
                val ageLabel = compactAgeLabel(daysInt)
                Text(
                    text = ageLabel,
                    style = MaterialTheme.typography.labelLarge,
                    color = ageColor,
                    fontWeight = if (daysInt != null) FontWeight.Bold else null,
                    maxLines = 1,
                    textAlign = TextAlign.End,
                    modifier = Modifier.widthIn(min = 64.dp),
                )
            } else {
                Column(
                    horizontalAlignment = Alignment.CenterHorizontally,
                    modifier = Modifier.width(64.dp),
                ) {
                    Text(
                        text = daysInt?.toString() ?: "—",
                        style = MaterialTheme.typography.titleLarge,
                        color = ageColor,
                        fontWeight = if (daysInt != null) FontWeight.Bold else null,
                    )
                    Text(
                        text = when {
                            daysInt == null -> ""
                            daysInt == 1 -> "day"
                            else -> "days"
                        },
                        style = MaterialTheme.typography.labelSmall,
                        color = ageColor,
                    )
                }
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
        if (!isCompact) {
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

@OptIn(kotlin.time.ExperimentalTime::class)
@Preview(showBackground = true)
@Composable
fun DeviceListItemCompactPreview() {
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
            density = DensityOption.COMPACT,
        )
    }
}

/** Compact rows stacked, to check that consecutive short cards read as a dense list. */
@OptIn(kotlin.time.ExperimentalTime::class)
@Preview(showBackground = true)
@Composable
fun DeviceListItemCompactListPreview() {
    BatteryButlerTheme {
        val nowInstant = Instant.parse("2026-01-18T17:00:00Z")
        val smokeType = DeviceType("type1", "Smoke Alarm", "detector_smoke")
        val coType = DeviceType("type2", "CO Detector", "detector_co")
        Column(
            verticalArrangement = androidx.compose.foundation.layout.Arrangement
                .spacedBy(Padding.medium),
        ) {
            DeviceListItem(
                device = Device(
                    "dev1",
                    "Kitchen Smoke",
                    "type1",
                    Instant.parse("2026-01-13T17:00:00Z"), // 5 days
                    nowInstant,
                    "Kitchen",
                ),
                deviceType = smokeType,
                onClick = {},
                nowInstant = nowInstant,
                density = DensityOption.COMPACT,
            )
            DeviceListItem(
                device = Device(
                    "dev2",
                    "Hallway CO Detector",
                    "type2",
                    Instant.parse("2025-07-02T17:00:00Z"), // 200 days
                    nowInstant,
                    "Hallway",
                ),
                deviceType = coType,
                onClick = {},
                nowInstant = nowInstant,
                density = DensityOption.COMPACT,
            )
            DeviceListItem(
                device = Device(
                    "dev3",
                    "Bedroom Smoke",
                    "type1",
                    Instant.parse("2024-12-02T17:00:00Z"), // 412 days
                    nowInstant,
                    "Bedroom",
                ),
                deviceType = smokeType,
                onClick = {},
                nowInstant = nowInstant,
                density = DensityOption.COMPACT,
            )
        }
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
