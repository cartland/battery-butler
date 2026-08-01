package com.chriscartland.batterybutler.presentationcore.components

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.layout.widthIn
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.BatteryFull
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import com.chriscartland.batterybutler.composeresources.composeStringResource
import com.chriscartland.batterybutler.composeresources.generated.resources.Res
import com.chriscartland.batterybutler.composeresources.generated.resources.content_desc_battery_replacement
import com.chriscartland.batterybutler.domain.model.BatteryEvent
import com.chriscartland.batterybutler.presentationcore.theme.BatteryButlerTheme
import com.chriscartland.batterybutler.presentationcore.theme.Padding
import com.chriscartland.batterybutler.presentationmodel.home.DensityOption
import kotlinx.datetime.TimeZone
import kotlinx.datetime.daysUntil
import kotlinx.datetime.toLocalDateTime
import kotlin.time.Clock
import kotlin.time.Instant

/**
 * A battery-replacement history row.
 *
 * Honours the same app-wide density as the other lists:
 * - [DensityOption.EXPANDED] (default): a 50.dp stacked month/day block, device name, a
 *   "type • location" secondary line, and a battery icon over "N days" in the trailing slot.
 * - [DensityOption.COMPACT]: a 24.dp single-line "MMM DD" date, no secondary line, and the age on
 *   one trailing line, so the card is only as tall as its single line of text.
 *
 * The date stays in the leading slot rather than collapsing away: a history list sorted by date is
 * unusable without it, which is why this row compacts differently from the device row.
 */
@OptIn(kotlin.time.ExperimentalTime::class)
@Composable
fun HistoryListItem(
    event: BatteryEvent,
    deviceName: String,
    deviceTypeName: String,
    deviceLocation: String?,
    onClick: () -> Unit,
    modifier: Modifier = Modifier,
    nowInstant: Instant = Clock.System.now(),
    density: DensityOption = DensityOption.EXPANDED,
) {
    val date = event.date.toLocalDateTime(TimeZone.currentSystemDefault())
    val month = date.month.name.take(3)
    val day = date.day.toString().padStart(2, '0')
    val daysAgo = calculateDaysAgo(event.date, nowInstant)
    val isCompact = density == DensityOption.COMPACT
    val ageLabel = if (daysAgo == 1) "1 day" else "$daysAgo days"

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
            if (isCompact) {
                Text(
                    text = "$month $day",
                    style = MaterialTheme.typography.labelLarge,
                    fontWeight = FontWeight.Bold,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                    maxLines = 1,
                )
            } else {
                Column(
                    modifier = Modifier
                        .size(50.dp)
                        .background(MaterialTheme.colorScheme.surfaceVariant, RoundedCornerShape(10.dp)),
                    verticalArrangement = Arrangement.Center,
                    horizontalAlignment = Alignment.CenterHorizontally,
                ) {
                    Text(
                        text = month,
                        style = MaterialTheme.typography.labelSmall,
                        fontWeight = FontWeight.Bold,
                        color = MaterialTheme.colorScheme.onSurfaceVariant,
                    )
                    Text(
                        text = day,
                        style = MaterialTheme.typography.titleMedium,
                        fontWeight = FontWeight.Bold,
                        color = MaterialTheme.colorScheme.onSurface,
                    )
                }
            }
        },
        trailing = {
            if (isCompact) {
                Text(
                    text = ageLabel,
                    style = MaterialTheme.typography.labelLarge,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                    maxLines = 1,
                    textAlign = TextAlign.End,
                    modifier = Modifier.widthIn(min = 64.dp),
                )
            } else {
                Column(
                    horizontalAlignment = Alignment.CenterHorizontally,
                    modifier = Modifier.width(60.dp),
                ) {
                    Icon(
                        imageVector = Icons.Default.BatteryFull,
                        contentDescription = composeStringResource(Res.string.content_desc_battery_replacement),
                        tint = MaterialTheme.colorScheme.onSurfaceVariant,
                    )
                    Text(
                        text = ageLabel,
                        style = MaterialTheme.typography.labelSmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant,
                    )
                }
            }
        },
    ) {
        Text(
            text = deviceName,
            style = MaterialTheme.typography.titleMedium,
            fontWeight = FontWeight.SemiBold,
            color = MaterialTheme.colorScheme.onSurface,
        )
        if (!isCompact) {
            val subText = if (deviceLocation != null) "$deviceTypeName • $deviceLocation" else deviceTypeName
            Text(
                text = subText,
                style = MaterialTheme.typography.bodyMedium,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
            )
        }
    }
}

@OptIn(kotlin.time.ExperimentalTime::class)
private fun calculateDaysAgo(
    eventDate: Instant,
    nowInstant: Instant,
): Int {
    val timeZone = TimeZone.currentSystemDefault()
    val now = nowInstant.toLocalDateTime(timeZone).date
    val date = eventDate.toLocalDateTime(timeZone).date
    return date.daysUntil(now)
}

@OptIn(kotlin.time.ExperimentalTime::class)
@Preview(showBackground = true)
@Composable
fun HistoryListItemPreview() {
    BatteryButlerTheme {
        // Use fixed dates for stable screenshots
        val nowInstant = Instant.parse("2026-01-18T17:00:00Z")
        val eventInstant = Instant.parse("2026-01-11T17:00:00Z") // 7 days ago
        val event = BatteryEvent("evt1", "dev1", eventInstant)
        HistoryListItem(
            event = event,
            deviceName = "Kitchen Smoke",
            deviceTypeName = "Smoke Alarm",
            deviceLocation = "Kitchen",
            onClick = {},
            nowInstant = nowInstant,
        )
    }
}
