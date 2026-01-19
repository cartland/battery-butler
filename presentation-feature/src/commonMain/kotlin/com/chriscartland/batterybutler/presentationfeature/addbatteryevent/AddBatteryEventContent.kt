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
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.AutoAwesome
import androidx.compose.material3.Button
import androidx.compose.material3.DropdownMenuItem
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.ExposedDropdownMenuBox
import androidx.compose.material3.ExposedDropdownMenuDefaults
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.MenuAnchorType
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import com.chriscartland.batterybutler.domain.ai.AiMessage
import com.chriscartland.batterybutler.domain.model.Device
import com.chriscartland.batterybutler.presentationcore.components.ButlerCenteredTopAppBar
import kotlinx.datetime.TimeZone
import kotlinx.datetime.atStartOfDayIn
import kotlinx.datetime.toLocalDateTime
import kotlin.time.Clock
import kotlin.time.Instant

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun AddBatteryEventContent(
    devices: List<Device>,
    aiMessages: List<AiMessage>,
    onAddEvent: (String, Instant) -> Unit, // deviceId, date
    onBatchAdd: (String) -> Unit,
    onAddDeviceClick: () -> Unit,
    onBack: () -> Unit,
    modifier: Modifier = Modifier,
) {
    var aiInput by remember { mutableStateOf("") }
    var deviceIdInput by remember { mutableStateOf("") }

    Scaffold(
        modifier = modifier,
        topBar = {
            ButlerCenteredTopAppBar(
                title = "Add Battery Event",
                onBack = onBack,
            )
        },
    ) { innerPadding ->
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(innerPadding)
                .padding(16.dp),
            verticalArrangement = Arrangement.spacedBy(16.dp),
        ) {
            // AI Section
            Text(
                "Batch Import (AI)",
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
                    placeholder = { Text("E.g. Replaced remote battery today") },
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
                    Icon(Icons.Default.AutoAwesome, contentDescription = "Process with AI")
                }
            }

            if (aiMessages.isNotEmpty()) {
                Text("AI Output:", style = MaterialTheme.typography.labelMedium)
                LazyColumn(
                    modifier = Modifier
                        .fillMaxWidth()
                        .height(200.dp) // Limited height
                        .padding(8.dp),
                ) {
                    items(aiMessages) { msg ->
                        Text(
                            text = "${msg.role}: ${msg.text}",
                            style = MaterialTheme.typography.bodySmall,
                            modifier = Modifier.padding(vertical = 4.dp),
                        )
                    }
                }
            }

            Spacer(modifier = Modifier.height(16.dp))

            // Manual Section
            Text(
                "Manual Entry",
                style = MaterialTheme.typography.titleMedium,
                color = MaterialTheme.colorScheme.primary,
            )

            var expanded by remember { mutableStateOf(false) }
            val selectedDevice = devices.find { it.id == deviceIdInput }

            // Date Selection
            val today = remember {
                Clock.System
                    .now()
                    .toLocalDateTime(TimeZone.currentSystemDefault())
                    .date
                    .toString()
            }
            var dateInput by remember { mutableStateOf(today) }

            ExposedDropdownMenuBox(
                expanded = expanded,
                onExpandedChange = { expanded = !expanded },
            ) {
                OutlinedTextField(
                    value = selectedDevice?.name ?: "Select Device",
                    onValueChange = {}, // ReadOnly
                    readOnly = true,
                    label = { Text("Device") },
                    trailingIcon = { ExposedDropdownMenuDefaults.TrailingIcon(expanded = expanded) },
                    modifier = Modifier.menuAnchor(MenuAnchorType.PrimaryNotEditable, true).fillMaxWidth(),
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
                        text = { Text("Add New Device...", fontWeight = FontWeight.Bold) },
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
                label = { Text("Date (YYYY-MM-DD)") },
                modifier = Modifier.fillMaxWidth(),
                singleLine = true,
            )

            Button(
                onClick = {
                    if (deviceIdInput.isNotBlank()) {
                        // Validate Date
                        val date = try {
                            kotlinx.datetime.LocalDate
                                .parse(dateInput)
                                .atStartOfDayIn(TimeZone.currentSystemDefault())
                                .let { Instant.fromEpochMilliseconds(it.toEpochMilliseconds()) }
                        } catch (e: Exception) {
                            Clock.System.now() // Fallback or handle error
                        }

                        onAddEvent(deviceIdInput, date)
                    }
                },
                enabled = deviceIdInput.isNotBlank(),
                modifier = Modifier.align(Alignment.End),
            ) {
                Text("Add Event")
            }
        }
    }
}
