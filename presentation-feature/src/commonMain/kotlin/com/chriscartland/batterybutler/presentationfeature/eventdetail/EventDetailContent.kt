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
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.Notes
import androidx.compose.material.icons.filled.BatteryFull
import androidx.compose.material.icons.filled.CalendarToday
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import com.chriscartland.batterybutler.composeresources.composeStringResource
import com.chriscartland.batterybutler.composeresources.generated.resources.Res
import com.chriscartland.batterybutler.composeresources.generated.resources.action_cancel
import com.chriscartland.batterybutler.composeresources.generated.resources.action_delete
import com.chriscartland.batterybutler.composeresources.generated.resources.action_delete_event
import com.chriscartland.batterybutler.composeresources.generated.resources.action_edit
import com.chriscartland.batterybutler.composeresources.generated.resources.content_desc_device_icon
import com.chriscartland.batterybutler.composeresources.generated.resources.dialog_delete_event_text
import com.chriscartland.batterybutler.composeresources.generated.resources.dialog_delete_event_title
import com.chriscartland.batterybutler.composeresources.generated.resources.error_event_not_found
import com.chriscartland.batterybutler.composeresources.generated.resources.event_detail_title
import com.chriscartland.batterybutler.composeresources.generated.resources.label_battery_type
import com.chriscartland.batterybutler.composeresources.generated.resources.label_notes
import com.chriscartland.batterybutler.composeresources.generated.resources.label_replaced_on
import com.chriscartland.batterybutler.composeresources.generated.resources.unknown_device
import com.chriscartland.batterybutler.composeresources.generated.resources.unknown_type
import com.chriscartland.batterybutler.domain.model.BatteryEvent
import com.chriscartland.batterybutler.domain.model.Device
import com.chriscartland.batterybutler.domain.model.DeviceType
import com.chriscartland.batterybutler.presentationcore.components.ButlerCenteredTopAppBar
import com.chriscartland.batterybutler.presentationcore.components.DeviceIconMapper
import com.chriscartland.batterybutler.presentationcore.components.DeviceListItem
import com.chriscartland.batterybutler.presentationcore.theme.BatteryButlerTheme
import com.chriscartland.batterybutler.presentationcore.theme.Padding
import com.chriscartland.batterybutler.presentationmodel.eventdetail.EventDetailScreenState
import kotlinx.datetime.TimeZone
import kotlinx.datetime.toLocalDateTime
import kotlin.time.Clock
import kotlin.time.ExperimentalTime
import kotlin.time.Instant

@OptIn(ExperimentalMaterial3Api::class, ExperimentalTime::class)
@Composable
fun EventDetailContent(
    uiState: EventDetailScreenState,
    onBack: () -> Unit,
    onEdit: () -> Unit,
    onDelete: () -> Unit,
    onDeviceClick: (String) -> Unit,
    modifier: Modifier = Modifier,
    nowInstant: Instant = Clock.System.now(),
) {
    var showDeleteDialog by rememberSaveable { mutableStateOf(false) }

    Scaffold(
        modifier = modifier,
        topBar = {
            ButlerCenteredTopAppBar(
                title = composeStringResource(Res.string.event_detail_title),
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
                EventDetailScreenState.Loading -> {
                    CircularProgressIndicator(modifier = Modifier.align(Alignment.Center))
                }

                EventDetailScreenState.NotFound -> {
                    Text(composeStringResource(Res.string.error_event_not_found), modifier = Modifier.align(Alignment.Center))
                }

                is EventDetailScreenState.Success -> {
                    EventDetailBody(
                        state = uiState,
                        onDeviceClick = onDeviceClick,
                        onDeleteClick = { showDeleteDialog = true },
                        nowInstant = nowInstant,
                    )
                }
            }
        }
    }

    if (showDeleteDialog) {
        AlertDialog(
            onDismissRequest = { showDeleteDialog = false },
            title = { Text(composeStringResource(Res.string.dialog_delete_event_title)) },
            text = { Text(composeStringResource(Res.string.dialog_delete_event_text)) },
            confirmButton = {
                TextButton(
                    onClick = {
                        onDelete()
                        showDeleteDialog = false
                    },
                    colors = ButtonDefaults.textButtonColors(contentColor = MaterialTheme.colorScheme.error),
                ) { Text(composeStringResource(Res.string.action_delete)) }
            },
            dismissButton = { TextButton(onClick = { showDeleteDialog = false }) { Text(composeStringResource(Res.string.action_cancel)) } },
        )
    }
}

@OptIn(ExperimentalTime::class)
@Composable
private fun EventDetailBody(
    state: EventDetailScreenState.Success,
    onDeviceClick: (String) -> Unit,
    onDeleteClick: () -> Unit,
    nowInstant: Instant = Clock.System.now(),
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
                .padding(vertical = Padding.large)
                .size(96.dp)
                .clip(CircleShape)
                .background(MaterialTheme.colorScheme.primaryContainer),
            contentAlignment = Alignment.Center,
        ) {
            Icon(
                imageVector = DeviceIconMapper.getIcon(iconName),
                contentDescription = composeStringResource(Res.string.content_desc_device_icon),
                modifier = Modifier.size(48.dp),
                tint = MaterialTheme.colorScheme.onPrimaryContainer,
            )
        }

        Text(
            text = state.device?.name ?: composeStringResource(Res.string.unknown_device),
            style = MaterialTheme.typography.titleLarge,
            fontWeight = FontWeight.Bold,
        )
        Text(
            text = state.deviceType?.name ?: composeStringResource(Res.string.unknown_type),
            style = MaterialTheme.typography.bodyMedium,
            color = MaterialTheme.colorScheme.onSurfaceVariant,
        )

        Spacer(modifier = Modifier.height(32.dp))

        // Date Row (read-only)
        DetailRow(
            icon = Icons.Default.CalendarToday,
            label = composeStringResource(Res.string.label_replaced_on),
            value = friendlyDate,
        )

        // Battery Type Row (only if non-null)
        event.batteryType?.let { type ->
            Spacer(modifier = Modifier.height(12.dp))
            DetailRow(
                icon = Icons.Default.BatteryFull,
                label = composeStringResource(Res.string.label_battery_type),
                value = type,
            )
        }

        // Notes Row (only if non-null)
        event.notes?.let { notes ->
            Spacer(modifier = Modifier.height(12.dp))
            DetailRow(
                icon = Icons.AutoMirrored.Filled.Notes,
                label = composeStringResource(Res.string.label_notes),
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
                nowInstant = nowInstant,
            )
        }

        Spacer(modifier = Modifier.height(24.dp))

        // Delete Button
        Button(
            onClick = onDeleteClick,
            colors = ButtonDefaults.buttonColors(
                containerColor = MaterialTheme.colorScheme.errorContainer,
                contentColor = MaterialTheme.colorScheme.error,
            ),
            modifier = Modifier.fillMaxWidth().height(56.dp),
            shape = MaterialTheme.shapes.medium,
        ) {
            Text(composeStringResource(Res.string.action_delete_event), fontWeight = FontWeight.SemiBold)
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
            .clip(MaterialTheme.shapes.medium)
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
        val state = EventDetailScreenState.Success(
            event = event,
            device = device,
            deviceType = type,
        )
        EventDetailContent(
            uiState = state,
            onBack = {},
            onEdit = {},
            onDelete = {},
            onDeviceClick = {},
            nowInstant = now,
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
        val state = EventDetailScreenState.Success(
            event = event,
            device = null,
            deviceType = null,
        )
        EventDetailContent(
            uiState = state,
            onBack = {},
            onEdit = {},
            onDelete = {},
            onDeviceClick = {},
            nowInstant = now,
        )
    }
}

@OptIn(ExperimentalTime::class)
@Preview(showBackground = true)
@Composable
fun EventDetailLoadingPreview() {
    BatteryButlerTheme {
        EventDetailContent(
            uiState = EventDetailScreenState.Loading,
            onBack = {},
            onEdit = {},
            onDelete = {},
            onDeviceClick = {},
            nowInstant = Instant.parse("2026-01-18T17:00:00Z"),
        )
    }
}

@OptIn(ExperimentalTime::class)
@Preview(showBackground = true)
@Composable
fun EventDetailContentNotFoundPreview() {
    BatteryButlerTheme {
        EventDetailContent(
            uiState = EventDetailScreenState.NotFound,
            onBack = {},
            onEdit = {},
            onDelete = {},
            onDeviceClick = {},
            nowInstant = Instant.parse("2026-01-18T17:00:00Z"),
        )
    }
}
