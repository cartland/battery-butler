package com.chriscartland.batterybutler.presenter.core.components

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
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
import androidx.compose.ui.unit.dp
import com.chriscartland.batterybutler.domain.model.BatteryEvent
import kotlinx.datetime.Clock
import kotlinx.datetime.TimeZone
import kotlinx.datetime.daysUntil
import kotlinx.datetime.toLocalDateTime

@Composable
fun HistoryListItem(
    event: BatteryEvent,
    deviceName: String,
    deviceTypeName: String,
    deviceLocation: String?,
    modifier: Modifier = Modifier,
) {
    val date = event.date.toLocalDateTime(TimeZone.currentSystemDefault())
    val month = date.month.name.take(3)
    val day = date.dayOfMonth.toString().padStart(2, '0')

    Row(
        modifier = modifier
            .fillMaxWidth()
            .padding(vertical = 8.dp)
            .background(MaterialTheme.colorScheme.surface, RoundedCornerShape(12.dp))
            .padding(16.dp),
        verticalAlignment = Alignment.CenterVertically,
    ) {
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

        Column(
            modifier = Modifier.weight(1f).padding(horizontal = 16.dp),
        ) {
            val timeZone = TimeZone.currentSystemDefault()
            val now = Clock.System
                .now()
                .toLocalDateTime(timeZone)
                .date
            val eventDate = event.date.toLocalDateTime(timeZone).date
            val daysAgo = eventDate.daysUntil(now)

            val relativeTime = when {
                daysAgo == 0 -> "Today"
                daysAgo == 1 -> "Yesterday"
                daysAgo < 30 -> "$daysAgo days ago"
                daysAgo < 365 -> {
                    val months = daysAgo / 30
                    if (months <= 1) "1 month ago" else "$months months ago"
                }
                else -> {
                    val years = daysAgo / 365
                    if (years <= 1) "1 year ago" else "$years years ago"
                }
            }

            Text(
                text = deviceName,
                style = MaterialTheme.typography.bodyLarge,
                fontWeight = FontWeight.Bold,
                color = MaterialTheme.colorScheme.onSurface,
            )
            val subText = if (deviceLocation != null) "$deviceTypeName • $deviceLocation" else deviceTypeName
            Text(
                text = subText,
                style = MaterialTheme.typography.bodyMedium,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
            )
        }

        Column(horizontalAlignment = Alignment.End) {
            Icon(
                imageVector = Icons.Default.BatteryFull, // Placeholder or specific icon
                contentDescription = null,
                tint = MaterialTheme.colorScheme.primary,
            )

            // Re-calculate relative time for the right side or just use the days?
            // "Right side should also be a battery with the age in days"
            val timeZone = TimeZone.currentSystemDefault()
            val now = Clock.System
                .now()
                .toLocalDateTime(timeZone)
                .date
            val eventDate = event.date.toLocalDateTime(timeZone).date
            val daysAgo = eventDate.daysUntil(now)

            Text(
                text = "$daysAgo days",
                style = MaterialTheme.typography.labelSmall,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
            )
        }
    }
}
