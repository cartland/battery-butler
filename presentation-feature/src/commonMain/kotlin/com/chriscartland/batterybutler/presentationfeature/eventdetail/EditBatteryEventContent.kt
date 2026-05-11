package com.chriscartland.batterybutler.presentationfeature.eventdetail

import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
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
import androidx.compose.foundation.text.KeyboardActions
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.CalendarToday
import androidx.compose.material.icons.filled.ChevronRight
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.DatePicker
import androidx.compose.material3.DatePickerDialog
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.material3.rememberDatePickerState
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.platform.LocalFocusManager
import androidx.compose.ui.semantics.Role
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.ImeAction
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import com.chriscartland.batterybutler.composeresources.composeStringResource
import com.chriscartland.batterybutler.composeresources.generated.resources.Res
import com.chriscartland.batterybutler.composeresources.generated.resources.action_cancel
import com.chriscartland.batterybutler.composeresources.generated.resources.action_delete
import com.chriscartland.batterybutler.composeresources.generated.resources.action_delete_event
import com.chriscartland.batterybutler.composeresources.generated.resources.action_ok
import com.chriscartland.batterybutler.composeresources.generated.resources.action_save
import com.chriscartland.batterybutler.composeresources.generated.resources.content_desc_calendar
import com.chriscartland.batterybutler.composeresources.generated.resources.content_desc_device_icon
import com.chriscartland.batterybutler.composeresources.generated.resources.dialog_delete_event_text
import com.chriscartland.batterybutler.composeresources.generated.resources.dialog_delete_event_title
import com.chriscartland.batterybutler.composeresources.generated.resources.edit_event_title
import com.chriscartland.batterybutler.composeresources.generated.resources.error_event_not_found
import com.chriscartland.batterybutler.composeresources.generated.resources.label_battery_type_optional
import com.chriscartland.batterybutler.composeresources.generated.resources.label_notes_optional
import com.chriscartland.batterybutler.composeresources.generated.resources.label_replacement_date
import com.chriscartland.batterybutler.domain.model.BatteryEvent
import com.chriscartland.batterybutler.domain.model.Device
import com.chriscartland.batterybutler.domain.model.DeviceType
import com.chriscartland.batterybutler.presentationcore.components.ButlerCenteredTopAppBar
import com.chriscartland.batterybutler.presentationcore.components.DeviceIconMapper
import com.chriscartland.batterybutler.presentationcore.theme.BatteryButlerTheme
import com.chriscartland.batterybutler.presentationcore.theme.Padding
import com.chriscartland.batterybutler.presentationmodel.eventdetail.EditBatteryEventScreenState
import kotlinx.datetime.TimeZone
import kotlinx.datetime.toLocalDateTime
import kotlin.time.ExperimentalTime
import kotlin.time.Instant

@OptIn(ExperimentalMaterial3Api::class, ExperimentalTime::class)
@Composable
fun EditBatteryEventContent(
    uiState: EditBatteryEventScreenState,
    onSave: (Instant, String?, String?) -> Unit,
    onDelete: () -> Unit,
    onBack: () -> Unit,
    modifier: Modifier = Modifier,
) {
    var dateMillis by rememberSaveable { mutableStateOf<Long?>(null) }
    var batteryType by rememberSaveable { mutableStateOf("") }
    var notes by rememberSaveable { mutableStateOf("") }
    var isInitialized by rememberSaveable { mutableStateOf(false) }
    var showDatePicker by rememberSaveable { mutableStateOf(false) }
    var showDeleteDialog by rememberSaveable { mutableStateOf(false) }
    val focusManager = LocalFocusManager.current

    Scaffold(
        modifier = modifier,
        topBar = {
            ButlerCenteredTopAppBar(
                title = composeStringResource(Res.string.edit_event_title),
                onBack = onBack,
                navigationIcon = {
                    TextButton(onClick = onBack) { Text(composeStringResource(Res.string.action_cancel)) }
                },
                actions = {
                    TextButton(
                        onClick = {
                            dateMillis?.let { millis ->
                                onSave(
                                    Instant.fromEpochMilliseconds(millis),
                                    batteryType.takeIf { it.isNotBlank() },
                                    notes.takeIf { it.isNotBlank() },
                                )
                            }
                        },
                        enabled = dateMillis != null,
                    ) {
                        Text(composeStringResource(Res.string.action_save), fontWeight = FontWeight.Bold)
                    }
                },
            )
        },
    ) { innerPadding ->
        Box(modifier = Modifier.fillMaxSize().padding(innerPadding)) {
            when (uiState) {
                EditBatteryEventScreenState.Loading -> {
                    CircularProgressIndicator(modifier = Modifier.align(Alignment.Center))
                }

                EditBatteryEventScreenState.NotFound -> {
                    Text(composeStringResource(Res.string.error_event_not_found), modifier = Modifier.align(Alignment.Center))
                }

                is EditBatteryEventScreenState.Success -> {
                    val event = uiState.event
                    LaunchedEffect(event) {
                        if (!isInitialized) {
                            dateMillis = event.date.toEpochMilliseconds()
                            batteryType = event.batteryType ?: ""
                            notes = event.notes ?: ""
                            isInitialized = true
                        }
                    }

                    val currentDate = dateMillis?.let {
                        val date = Instant
                            .fromEpochMilliseconds(it)
                            .toLocalDateTime(TimeZone.currentSystemDefault())
                        "${date.year}-${(date.month.ordinal + 1).toString().padStart(2, '0')}-${date.day.toString().padStart(2, '0')}"
                    } ?: ""

                    if (showDatePicker) {
                        val datePickerState = rememberDatePickerState(
                            initialSelectedDateMillis = dateMillis,
                        )
                        DatePickerDialog(
                            onDismissRequest = { showDatePicker = false },
                            confirmButton = {
                                TextButton(
                                    onClick = {
                                        datePickerState.selectedDateMillis?.let { millis ->
                                            dateMillis = millis
                                        }
                                        showDatePicker = false
                                    },
                                ) {
                                    Text(composeStringResource(Res.string.action_ok))
                                }
                            },
                            dismissButton = {
                                TextButton(onClick = { showDatePicker = false }) {
                                    Text(composeStringResource(Res.string.action_cancel))
                                }
                            },
                        ) {
                            DatePicker(state = datePickerState)
                        }
                    }

                    Column(
                        modifier = Modifier
                            .fillMaxSize()
                            .verticalScroll(rememberScrollState())
                            .padding(Padding.standard),
                    ) {
                        // Context header
                        val iconName = uiState.deviceType?.defaultIcon ?: "devices_other"
                        Row(
                            verticalAlignment = Alignment.CenterVertically,
                            horizontalArrangement = Arrangement.spacedBy(12.dp),
                            modifier = Modifier.padding(bottom = 24.dp),
                        ) {
                            Box(
                                modifier = Modifier
                                    .size(48.dp)
                                    .clip(CircleShape)
                                    .background(MaterialTheme.colorScheme.primaryContainer),
                                contentAlignment = Alignment.Center,
                            ) {
                                Icon(
                                    imageVector = DeviceIconMapper.getIcon(iconName),
                                    contentDescription = composeStringResource(Res.string.content_desc_device_icon),
                                    modifier = Modifier.size(28.dp),
                                    tint = MaterialTheme.colorScheme.onPrimaryContainer,
                                )
                            }
                            Column {
                                Text(
                                    text = uiState.device?.name ?: "Unknown Device",
                                    style = MaterialTheme.typography.titleMedium,
                                    fontWeight = FontWeight.SemiBold,
                                )
                                Text(
                                    text = uiState.deviceType?.name ?: "Unknown",
                                    style = MaterialTheme.typography.bodyMedium,
                                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                                )
                            }
                        }

                        // Date Row
                        Row(
                            modifier = Modifier
                                .fillMaxWidth()
                                .clip(MaterialTheme.shapes.medium)
                                .background(MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.5f))
                                .clickable(role = Role.Button) { showDatePicker = true }
                                .padding(Padding.standard),
                            verticalAlignment = Alignment.CenterVertically,
                            horizontalArrangement = Arrangement.SpaceBetween,
                        ) {
                            Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(12.dp)) {
                                Icon(Icons.Default.CalendarToday, contentDescription = composeStringResource(Res.string.content_desc_calendar), tint = MaterialTheme.colorScheme.primary)
                                Text(composeStringResource(Res.string.label_replacement_date), fontWeight = FontWeight.Medium)
                            }
                            Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                                Text(currentDate, color = MaterialTheme.colorScheme.primary, fontWeight = FontWeight.Bold)
                                Icon(
                                    Icons.Default.ChevronRight,
                                    contentDescription = null,
                                    tint = MaterialTheme.colorScheme.onSurfaceVariant,
                                )
                            }
                        }

                        Spacer(modifier = Modifier.height(16.dp))

                        OutlinedTextField(
                            value = batteryType,
                            onValueChange = { batteryType = it },
                            label = { Text(composeStringResource(Res.string.label_battery_type_optional)) },
                            modifier = Modifier.fillMaxWidth(),
                            singleLine = true,
                            keyboardOptions = KeyboardOptions(imeAction = ImeAction.Next),
                            keyboardActions = KeyboardActions(onNext = { focusManager.clearFocus() }),
                        )

                        Spacer(modifier = Modifier.height(16.dp))

                        OutlinedTextField(
                            value = notes,
                            onValueChange = { notes = it },
                            label = { Text(composeStringResource(Res.string.label_notes_optional)) },
                            modifier = Modifier.fillMaxWidth(),
                            minLines = 3,
                            keyboardOptions = KeyboardOptions(imeAction = ImeAction.Done),
                            keyboardActions = KeyboardActions(onDone = { focusManager.clearFocus() }),
                        )

                        Spacer(modifier = Modifier.height(24.dp))

                        // Delete Button
                        Button(
                            onClick = { showDeleteDialog = true },
                            colors = ButtonDefaults.buttonColors(
                                containerColor = MaterialTheme.colorScheme.errorContainer,
                                contentColor = MaterialTheme.colorScheme.error,
                            ),
                            modifier = Modifier.fillMaxWidth().height(56.dp),
                            shape = MaterialTheme.shapes.medium,
                        ) {
                            Text(composeStringResource(Res.string.action_delete_event), fontWeight = FontWeight.SemiBold)
                        }
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
}

@OptIn(ExperimentalTime::class)
@Preview(showBackground = true)
@Composable
fun EditBatteryEventContentPreview() {
    BatteryButlerTheme {
        val now = Instant.parse("2026-01-18T17:00:00Z")
        val event = BatteryEvent("evt1", "dev1", now, batteryType = "AA", notes = "Replaced with rechargeable")
        val type = DeviceType("type1", "Smoke Alarm", "detector_smoke")
        val device = Device("dev1", "Kitchen Smoke", "type1", now, now, "Kitchen")
        val state = EditBatteryEventScreenState.Success(
            event = event,
            device = device,
            deviceType = type,
        )
        EditBatteryEventContent(
            uiState = state,
            onSave = { _, _, _ -> },
            onDelete = {},
            onBack = {},
        )
    }
}

@Preview(showBackground = true)
@Composable
fun EditBatteryEventNotFoundPreview() {
    BatteryButlerTheme {
        EditBatteryEventContent(
            uiState = EditBatteryEventScreenState.NotFound,
            onSave = { _, _, _ -> },
            onDelete = {},
            onBack = {},
        )
    }
}

@Preview(showBackground = true)
@Composable
fun EditBatteryEventLoadingPreview() {
    BatteryButlerTheme {
        EditBatteryEventContent(
            uiState = EditBatteryEventScreenState.Loading,
            onSave = { _, _, _ -> },
            onDelete = {},
            onBack = {},
        )
    }
}
