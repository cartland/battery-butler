package com.chriscartland.batterybutler.presentationfeature.eventdetail

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.BatteryFull
import androidx.compose.material.icons.filled.CalendarToday
import androidx.compose.material.icons.filled.Notes
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import com.chriscartland.batterybutler.composeresources.composeStringResource
import com.chriscartland.batterybutler.composeresources.generated.resources.Res
import com.chriscartland.batterybutler.composeresources.generated.resources.action_edit
import com.chriscartland.batterybutler.domain.model.BatteryEvent
import com.chriscartland.batterybutler.domain.model.Device
import com.chriscartland.batterybutler.domain.model.DeviceType
import com.chriscartland.batterybutler.presentationcore.components.ButlerCenteredTopAppBar
import com.chriscartland.batterybutler.presentationcore.components.DeviceIconMapper
import com.chriscartland.batterybutler.presentationcore.components.DeviceListItem
import com.chriscartland.batterybutler.presentationcore.theme.BatteryButlerTheme
import com.chriscartland.batterybutler.presentationcore.theme.Padding
import com.chriscartland.batterybutler.presentationmodel.eventdetail.EventDetailUiState
import kotlinx.datetime.TimeZone
import kotlinx.datetime.toLocalDateTime
import kotlin.time.ExperimentalTime
import kotlin.time.Instant

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun EventDetailContent(
    uiState: EventDetailUiState,
    onBack: () -> Unit,
    onEdit: () -> Unit,
    onDeviceClick: (String) -> Unit,
    modifier: Modifier = Modifier,
) {
    Scaffold(
        modifier = modifier,
        topBar = {
            ButlerCenteredTopAppBar(
                title = "Event Detail",
                onBack = onBack,
                actions = {
                    TextButton(onClick = onEdit) {
                        Text(
                            composeStringResource(Res.string.action_edit),
                            color = MaterialTheme.colorScheme.primary,
                            fontWeight = FontWeight.SemiBold,
                        )
                    }
                },
            )
        },
    ) { innerPadding ->
        Box(modifier = Modifier.fillMaxSize().padding(innerPadding)) {
            when (uiState) {
                EventDetailUiState.Loading -> {
                    CircularProgressIndicator(modifier = Modifier.align(Alignment.Center))
                }
                EventDetailUiState.NotFound -> {
                    Text("Event not found", modifier = Modifier.align(Alignment.Center))
                }
                is EventDetailUiState.Success -> {
                    EventDetailBody(
                        state = uiState,
                        onDeviceClick = onDeviceClick,
                    )
                }
            }
        }
    }
}

@OptIn(ExperimentalTime::class)
@Composable
private fun EventDetailBody(
    state: EventDetailUiState.Success,
    onDeviceClick: (String) -> Unit,
) {
    val event = state.event
    val date = event.date.toLocalDateTime(TimeZone.currentSystemDefault())
    val friendlyDate = "${date.month.name.take(3).lowercase().replaceFirstChar { it.uppercase() }} ${date.day}, ${date.year}"

    Column(
        modifier = Modifier
            .fillMaxSize()
            .verticalScroll(rememberScrollState())
            .padding(Padding.standard),
        horizontalAlignment = Alignment.CenterHorizontally,
    ) {
        // Profile Header
        val iconName = state.deviceType?.defaultIcon ?: "devices_other"
        Box(
            modifier = Modifier
                .padding(vertical = 24.dp)
                .size(96.dp)
                .clip(CircleShape)
                .background(MaterialTheme.colorScheme.primaryContainer),
            contentAlignment = Alignment.Center,
        ) {
            Icon(
                imageVector = DeviceIconMapper.getIcon(iconName),
                contentDescription = "Device icon",
                modifier = Modifier.size(48.dp),
                tint = MaterialTheme.colorScheme.onPrimaryContainer,
            )
        }

        Text(
            text = state.device?.name ?: "Unknown Device",
            style = MaterialTheme.typography.titleLarge,
            fontWeight = FontWeight.Bold,
        )
        Text(
            text = state.deviceType?.name ?: "Unknown",
            style = MaterialTheme.typography.bodyMedium,
            color = MaterialTheme.colorScheme.onSurfaceVariant,
        )

        Spacer(modifier = Modifier.height(32.dp))

        // Date Row (read-only)
        DetailRow(
            icon = Icons.Default.CalendarToday,
            label = "Replaced On",
            value = friendlyDate,
        )

        // Battery Type Row (only if non-null)
        event.batteryType?.let { type ->
            Spacer(modifier = Modifier.height(12.dp))
            DetailRow(
                icon = Icons.Default.BatteryFull,
                label = "Battery Type",
                value = type,
            )
        }

        // Notes Row (only if non-null)
        event.notes?.let { notes ->
            Spacer(modifier = Modifier.height(12.dp))
            DetailRow(
                icon = Icons.Default.Notes,
                label = "Notes",
                value = notes,
            )
        }

        // Device card (tappable, if device exists)
        state.device?.let { device ->
            Spacer(modifier = Modifier.height(24.dp))
            DeviceListItem(
                device = device,
                deviceType = state.deviceType,
                onClick = { onDeviceClick(device.id) },
            )
        }

        Spacer(modifier = Modifier.height(32.dp))
    }
}

@Composable
private fun DetailRow(
    icon: androidx.compose.ui.graphics.vector.ImageVector,
    label: String,
    value: String,
    modifier: Modifier = Modifier,
) {
    Row(
        modifier = modifier
            .fillMaxWidth()
            .clip(RoundedCornerShape(12.dp))
            .background(MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.5f))
            .padding(Padding.standard),
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.SpaceBetween,
    ) {
        Row(
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.spacedBy(12.dp),
        ) {
            Icon(icon, contentDescription = label, tint = MaterialTheme.colorScheme.primary)
            Text(label, fontWeight = FontWeight.Medium)
        }
        Text(
            value,
            color = MaterialTheme.colorScheme.onSurface,
            fontWeight = FontWeight.Bold,
        )
    }
}

@OptIn(ExperimentalTime::class)
@Preview
@Composable
fun EventDetailContentPreview() {
    BatteryButlerTheme {
        val now = Instant.parse("2026-01-18T17:00:00Z")
        val event = BatteryEvent("evt1", "dev1", now, batteryType = "AA", notes = "Replaced with rechargeable")
        val type = DeviceType("type1", "Smoke Alarm", "detector_smoke")
        val device = Device("dev1", "Kitchen Smoke", "type1", now, now, "Kitchen")
        val state = EventDetailUiState.Success(
            event = event,
            device = device,
            deviceType = type,
        )
        EventDetailContent(
            uiState = state,
            onBack = {},
            onEdit = {},
            onDeviceClick = {},
        )
    }
}

@OptIn(ExperimentalTime::class)
@Preview
@Composable
fun EventDetailContentDeletedDevicePreview() {
    BatteryButlerTheme {
        val now = Instant.parse("2026-01-18T17:00:00Z")
        val event = BatteryEvent("evt1", "dev1", now)
        val state = EventDetailUiState.Success(
            event = event,
            device = null,
            deviceType = null,
        )
        EventDetailContent(
            uiState = state,
            onBack = {},
            onEdit = {},
            onDeviceClick = {},
        )
    }
}

@Preview(showBackground = true)
@Composable
fun EventDetailContentNotFoundPreview() {
    BatteryButlerTheme {
        EventDetailContent(
            uiState = EventDetailUiState.NotFound,
            onBack = {},
            onEdit = {},
            onDeviceClick = {},
        )
    }
}
