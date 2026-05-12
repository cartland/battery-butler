package com.chriscartland.batterybutler.presentationfeature.addbatteryevent

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.text.KeyboardActions
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.AutoAwesome
import androidx.compose.material3.Button
import androidx.compose.material3.DropdownMenuItem
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.ExposedDropdownMenuAnchorType
import androidx.compose.material3.ExposedDropdownMenuBox
import androidx.compose.material3.ExposedDropdownMenuDefaults
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalFocusManager
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.ImeAction
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import com.chriscartland.batterybutler.composeresources.composeStringResource
import com.chriscartland.batterybutler.composeresources.generated.resources.Res
import com.chriscartland.batterybutler.composeresources.generated.resources.action_add_event
import com.chriscartland.batterybutler.composeresources.generated.resources.action_add_new_device
import com.chriscartland.batterybutler.composeresources.generated.resources.action_batch_import_ai
import com.chriscartland.batterybutler.composeresources.generated.resources.action_hide_details
import com.chriscartland.batterybutler.composeresources.generated.resources.action_more_details
import com.chriscartland.batterybutler.composeresources.generated.resources.action_process_ai
import com.chriscartland.batterybutler.composeresources.generated.resources.add_event_title
import com.chriscartland.batterybutler.composeresources.generated.resources.label_ai_output
import com.chriscartland.batterybutler.composeresources.generated.resources.label_battery_type_optional
import com.chriscartland.batterybutler.composeresources.generated.resources.label_date_format
import com.chriscartland.batterybutler.composeresources.generated.resources.label_device
import com.chriscartland.batterybutler.composeresources.generated.resources.label_manual_entry
import com.chriscartland.batterybutler.composeresources.generated.resources.label_notes_optional
import com.chriscartland.batterybutler.composeresources.generated.resources.label_select_device
import com.chriscartland.batterybutler.composeresources.generated.resources.placeholder_battery_event_ai
import com.chriscartland.batterybutler.domain.model.BatchOperationResult
import com.chriscartland.batterybutler.domain.model.Device
import com.chriscartland.batterybutler.presentationcore.components.ButlerCenteredTopAppBar
import com.chriscartland.batterybutler.presentationcore.theme.BatteryButlerTheme
import com.chriscartland.batterybutler.presentationcore.theme.Padding
import kotlinx.datetime.TimeZone
import kotlinx.datetime.atStartOfDayIn
import kotlinx.datetime.toLocalDateTime
import kotlin.time.Clock
import kotlin.time.ExperimentalTime
import kotlin.time.Instant

@OptIn(ExperimentalMaterial3Api::class, ExperimentalTime::class)
@Composable
fun AddBatteryEventContent(
    devices: List<Device>,
    aiMessages: List<BatchOperationResult>,
    isAiBatchImportEnabled: Boolean,
    onAddEvent: (String, Instant, String?, String?) -> Unit, // deviceId, date, batteryType, notes
    onBatchAdd: (String) -> Unit,
    onAddDeviceClick: () -> Unit,
    onBack: () -> Unit,
    modifier: Modifier = Modifier,
    initialDate: String = Clock.System
        .now()
        .toLocalDateTime(TimeZone.currentSystemDefault())
        .date
        .toString(),
) {
    var aiInput by remember { mutableStateOf("") }
    var deviceIdInput by remember { mutableStateOf("") }
    var showMoreDetails by remember { mutableStateOf(false) }
    var batteryTypeInput by remember { mutableStateOf("") }
    var notesInput by remember { mutableStateOf("") }
    val focusManager = LocalFocusManager.current

    Scaffold(
        modifier = modifier,
        topBar = {
            ButlerCenteredTopAppBar(
                title = composeStringResource(Res.string.add_event_title),
                onBack = onBack,
            )
        },
    ) { innerPadding ->
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(innerPadding)
                .padding(Padding.standard),
            verticalArrangement = Arrangement.spacedBy(Padding.standard),
        ) {
            // AI Section (only shown when AI is available)
            if (isAiBatchImportEnabled) {
                Text(
                    composeStringResource(Res.string.action_batch_import_ai),
                    style = MaterialTheme.typography.titleMedium,
                    color = MaterialTheme.colorScheme.primary,
                )

                Row(
                    modifier = Modifier.fillMaxWidth(),
                    verticalAlignment = Alignment.CenterVertically,
                ) {
                    OutlinedTextField(
                        value = aiInput,
                        onValueChange = { aiInput = it },
                        modifier = Modifier.weight(1f),
                        placeholder = { Text(composeStringResource(Res.string.placeholder_battery_event_ai)) },
                        maxLines = 3,
                    )
                    IconButton(
                        onClick = {
                            if (aiInput.isNotBlank()) {
                                onBatchAdd(aiInput)
                                aiInput = ""
                            }
                        },
                        enabled = aiInput.isNotBlank(),
                    ) {
                        Icon(Icons.Default.AutoAwesome, contentDescription = composeStringResource(Res.string.action_process_ai))
                    }
                }

                if (aiMessages.isNotEmpty()) {
                    Text(composeStringResource(Res.string.label_ai_output), style = MaterialTheme.typography.labelMedium)
                    LazyColumn(
                        modifier = Modifier
                            .fillMaxWidth()
                            .height(200.dp) // Limited height
                            .padding(Padding.small),
                    ) {
                        items(aiMessages) { msg ->
                            val text = when (msg) {
                                is BatchOperationResult.Progress -> "🤖 ${msg.message}"
                                is BatchOperationResult.Success -> "✅ ${msg.message}"
                                is BatchOperationResult.Error -> "❌ ${msg.error.message}"
                            }
                            val color = when (msg) {
                                is BatchOperationResult.Error -> MaterialTheme.colorScheme.error
                                is BatchOperationResult.Success -> MaterialTheme.colorScheme.primary
                                is BatchOperationResult.Progress -> MaterialTheme.colorScheme.onSurface
                            }
                            Text(
                                text = text,
                                style = MaterialTheme.typography.bodySmall,
                                color = color,
                                modifier = Modifier.padding(vertical = Padding.extraSmall),
                            )
                        }
                    }
                }

                Spacer(modifier = Modifier.height(16.dp))
            }

            // Manual Section
            Text(
                composeStringResource(Res.string.label_manual_entry),
                style = MaterialTheme.typography.titleMedium,
                color = MaterialTheme.colorScheme.primary,
            )

            var expanded by remember { mutableStateOf(false) }
            val selectedDevice = devices.find { it.id == deviceIdInput }

            // Date Selection
            var dateInput by remember { mutableStateOf(initialDate) }

            ExposedDropdownMenuBox(
                expanded = expanded,
                onExpandedChange = { expanded = !expanded },
            ) {
                OutlinedTextField(
                    value = selectedDevice?.name ?: composeStringResource(Res.string.label_select_device),
                    onValueChange = {}, // ReadOnly
                    readOnly = true,
                    label = { Text(composeStringResource(Res.string.label_device)) },
                    trailingIcon = { ExposedDropdownMenuDefaults.TrailingIcon(expanded = expanded) },
                    modifier = Modifier.menuAnchor(ExposedDropdownMenuAnchorType.PrimaryNotEditable, true).fillMaxWidth(),
                )
                ExposedDropdownMenu(
                    expanded = expanded,
                    onDismissRequest = { expanded = false },
                ) {
                    devices.forEach { device ->
                        DropdownMenuItem(
                            text = { Text(device.name) },
                            onClick = {
                                deviceIdInput = device.id
                                expanded = false
                            },
                        )
                    }
                    HorizontalDivider()
                    DropdownMenuItem(
                        text = { Text(composeStringResource(Res.string.action_add_new_device), fontWeight = FontWeight.Bold) },
                        onClick = {
                            onAddDeviceClick()
                            expanded = false
                        },
                    )
                }
            }

            OutlinedTextField(
                value = dateInput,
                onValueChange = { dateInput = it },
                label = { Text(composeStringResource(Res.string.label_date_format)) },
                modifier = Modifier.fillMaxWidth(),
                singleLine = true,
                keyboardOptions = KeyboardOptions(imeAction = ImeAction.Done),
                keyboardActions = KeyboardActions(onDone = { focusManager.clearFocus() }),
            )

            // Expandable "More Details" section
            TextButton(
                onClick = { showMoreDetails = !showMoreDetails },
            ) {
                Text(
                    if (showMoreDetails) {
                        composeStringResource(Res.string.action_hide_details)
                    } else {
                        composeStringResource(Res.string.action_more_details)
                    },
                    style = MaterialTheme.typography.labelLarge,
                )
            }

            if (showMoreDetails) {
                OutlinedTextField(
                    value = batteryTypeInput,
                    onValueChange = { batteryTypeInput = it },
                    label = { Text(composeStringResource(Res.string.label_battery_type_optional)) },
                    modifier = Modifier.fillMaxWidth(),
                    singleLine = true,
                    keyboardOptions = KeyboardOptions(imeAction = ImeAction.Next),
                )

                OutlinedTextField(
                    value = notesInput,
                    onValueChange = { notesInput = it },
                    label = { Text(composeStringResource(Res.string.label_notes_optional)) },
                    modifier = Modifier.fillMaxWidth(),
                    minLines = 2,
                    keyboardOptions = KeyboardOptions(imeAction = ImeAction.Done),
                    keyboardActions = KeyboardActions(onDone = { focusManager.clearFocus() }),
                )
            }

            Button(
                onClick = {
                    if (deviceIdInput.isNotBlank()) {
                        // Validate Date - parse and convert to Instant at start of day
                        val date = try {
                            kotlinx.datetime.LocalDate
                                .parse(dateInput)
                                .atStartOfDayIn(TimeZone.currentSystemDefault())
                        } catch (_: Exception) {
                            Clock.System.now() // Fallback if date parsing fails
                        }

                        onAddEvent(
                            deviceIdInput,
                            date,
                            batteryTypeInput.takeIf { it.isNotBlank() },
                            notesInput.takeIf { it.isNotBlank() },
                        )
                    }
                },
                enabled = deviceIdInput.isNotBlank(),
                modifier = Modifier.align(Alignment.End),
            ) {
                Text(composeStringResource(Res.string.action_add_event))
            }
        }
    }
}

@Preview(showBackground = true)
@Composable
fun AddBatteryEventContentEmptyPreview() {
    BatteryButlerTheme {
        AddBatteryEventContent(
            devices = emptyList(),
            aiMessages = emptyList(),
            isAiBatchImportEnabled = true,
            onAddEvent = { _, _, _, _ -> },
            onBatchAdd = {},
            onAddDeviceClick = {},
            onBack = {},
            initialDate = "2026-01-18",
        )
    }
}

@OptIn(ExperimentalTime::class)
@Preview
@Composable
fun AddBatteryEventContentPreview() {
    BatteryButlerTheme {
        val now = Instant.parse("2026-01-18T17:00:00Z")
        val device = Device("dev1", "Kitchen Smoke", "type1", now, now, "Kitchen")
        AddBatteryEventContent(
            devices = listOf(device),
            aiMessages = emptyList(),
            isAiBatchImportEnabled = true,
            onAddEvent = { _, _, _, _ -> },
            onBatchAdd = {},
            onAddDeviceClick = {},
            onBack = {},
            initialDate = "2026-01-18", // Use fixed dates for stable screenshots
        )
    }
}
