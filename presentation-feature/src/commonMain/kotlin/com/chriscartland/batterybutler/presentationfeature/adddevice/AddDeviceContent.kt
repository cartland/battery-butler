package com.chriscartland.batterybutler.presentationfeature.adddevice

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.IntrinsicSize
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.KeyboardActions
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.AutoAwesome
import androidx.compose.material.icons.filled.DevicesOther
import androidx.compose.material3.DropdownMenuItem
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.ExposedDropdownMenuBox
import androidx.compose.material3.ExposedDropdownMenuDefaults
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.ExposedDropdownMenuAnchorType
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.focus.FocusDirection
import androidx.compose.ui.platform.LocalFocusManager
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.ImeAction
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import com.chriscartland.batterybutler.domain.ai.AiMessage
import com.chriscartland.batterybutler.domain.ai.AiRole
import com.chriscartland.batterybutler.domain.model.DeviceInput
import com.chriscartland.batterybutler.domain.model.DeviceType
import com.chriscartland.batterybutler.presentationcore.components.ButlerCenteredTopAppBar
import com.chriscartland.batterybutler.presentationcore.components.DeviceIconMapper
import com.chriscartland.batterybutler.presentationcore.theme.BatteryButlerTheme

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun AddDeviceContent(
    deviceTypes: List<DeviceType>,
    aiMessages: List<AiMessage>,
    onAddDevice: (DeviceInput) -> Unit,
    onBatchAdd: (String) -> Unit,
    onManageDeviceTypesClick: () -> Unit,
    onBack: () -> Unit,
    modifier: Modifier = Modifier,
) {
    var name by rememberSaveable { mutableStateOf("") }
    var location by rememberSaveable { mutableStateOf("") }
    var selectedType by remember { mutableStateOf<DeviceType?>(null) }
    var expanded by remember { mutableStateOf(false) }
    val focusManager = LocalFocusManager.current

    Scaffold(
        modifier = modifier,
        topBar = {
            ButlerCenteredTopAppBar(
                title = "Add Device",
                onBack = onBack,
                navigationIcon = {
                    TextButton(onClick = onBack) {
                        Text("Cancel", color = MaterialTheme.colorScheme.primary)
                    }
                },
                actions = {
                    TextButton(onClick = {
                        if (name.isNotBlank() && selectedType != null) {
                            onAddDevice(
                                DeviceInput(
                                    name = name,
                                    location = location.takeIf { it.isNotBlank() },
                                    typeId = selectedType!!.id,
                                ),
                            )
                        }
                    }) {
                        Text(
                            "Save",
                            color = if (name.isNotBlank() && selectedType != null) {
                                MaterialTheme.colorScheme.primary
                            } else {
                                MaterialTheme.colorScheme.onSurface.copy(alpha = 0.38f)
                            },
                            fontWeight = FontWeight.Bold,
                        )
                    }
                },
            )
        },
    ) { innerPadding ->

        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(innerPadding)
                .verticalScroll(rememberScrollState())
                .padding(16.dp),
            verticalArrangement = Arrangement.spacedBy(24.dp),
            horizontalAlignment = Alignment.CenterHorizontally,
        ) {
            AddDeviceAiSection(aiMessages = aiMessages, onBatchAdd = onBatchAdd)
            AddDeviceManualSection(
                name = name,
                onNameChange = { name = it },
                location = location,
                onLocationChange = { location = it },
                deviceTypes = deviceTypes,
                selectedType = selectedType,
                onTypeSelected = { selectedType = it },
                onManageDeviceTypesClick = onManageDeviceTypesClick,
            )
        }
    }
}

@Composable
fun AddDeviceAiSection(
    aiMessages: List<AiMessage>,
    onBatchAdd: (String) -> Unit,
) {
    // AI Section
    Column(modifier = Modifier.fillMaxWidth()) {
        Text(
            "Batch Import (AI)",
            style = MaterialTheme.typography.titleMedium,
            color = MaterialTheme.colorScheme.primary,
            modifier = Modifier.padding(bottom = 8.dp),
        )

        var aiInput by rememberSaveable { mutableStateOf("") }
        Row(
            modifier = Modifier.fillMaxWidth(),
            verticalAlignment = Alignment.CenterVertically,
        ) {
            OutlinedTextField(
                value = aiInput,
                onValueChange = { aiInput = it },
                modifier = Modifier.weight(1f),
                placeholder = { Text("E.g. Add Fire Alarm in Hallway") },
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
            Text("AI Output:", style = MaterialTheme.typography.labelMedium, modifier = Modifier.padding(top = 8.dp))
            LazyColumn(
                modifier = Modifier
                    .fillMaxWidth()
                    .height(150.dp)
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
    }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun AddDeviceManualSection(
    name: String,
    onNameChange: (String) -> Unit,
    location: String,
    onLocationChange: (String) -> Unit,
    deviceTypes: List<DeviceType>,
    selectedType: DeviceType?,
    onTypeSelected: (DeviceType) -> Unit,
    onManageDeviceTypesClick: () -> Unit,
) {
    var expanded by remember { mutableStateOf(false) }
    val focusManager = LocalFocusManager.current

    // Manual Section
    Column(
        verticalArrangement = Arrangement.spacedBy(16.dp),
        modifier = Modifier.fillMaxWidth(),
    ) {
        Text(
            "Manual Entry",
            style = MaterialTheme.typography.titleMedium,
            color = MaterialTheme.colorScheme.primary,
        )
        OutlinedTextField(
            value = name,
            onValueChange = onNameChange,
            label = { Text("Device Name") },
            modifier = Modifier.fillMaxWidth(),
            shape = RoundedCornerShape(12.dp),
            keyboardOptions = KeyboardOptions(imeAction = ImeAction.Next),
            keyboardActions = KeyboardActions(
                onNext = { focusManager.moveFocus(FocusDirection.Next) },
            ),
        )

        OutlinedTextField(
            value = location,
            onValueChange = onLocationChange,
            label = { Text("Location") },
            modifier = Modifier.fillMaxWidth(),
            shape = RoundedCornerShape(12.dp),
            keyboardOptions = KeyboardOptions(imeAction = ImeAction.Done),
            keyboardActions = KeyboardActions(
                onDone = { focusManager.clearFocus() },
            ),
        )

        // Device Type Selection
        Row(
            modifier = Modifier.fillMaxWidth().height(IntrinsicSize.Min),
            horizontalArrangement = Arrangement.spacedBy(8.dp),
            verticalAlignment = Alignment.Bottom,
        ) {
            ExposedDropdownMenuBox(
                expanded = expanded,
                onExpandedChange = { expanded = !expanded },
                modifier = Modifier.weight(1f),
            ) {
                OutlinedTextField(
                    value = selectedType?.name ?: "",
                    onValueChange = {},
                    label = { Text("Device Type") },
                    leadingIcon = if (selectedType != null) {
                        {
                            Icon(
                                imageVector = DeviceIconMapper.getIcon(
                                    selectedType.defaultIcon,
                                ),
                                contentDescription = null,
                            )
                        }
                    } else {
                        null
                    },
                    readOnly = true,
                    trailingIcon = { ExposedDropdownMenuDefaults.TrailingIcon(expanded = expanded) },
                    modifier = Modifier
                        .menuAnchor(ExposedDropdownMenuAnchorType.PrimaryNotEditable)
                        .fillMaxWidth(),
                    shape = RoundedCornerShape(12.dp),
                    colors = ExposedDropdownMenuDefaults.outlinedTextFieldColors(),
                )

                ExposedDropdownMenu(
                    expanded = expanded,
                    onDismissRequest = { expanded = false },
                ) {
                    deviceTypes.forEach { type ->
                        DropdownMenuItem(
                            text = { Text(type.name) },
                            leadingIcon = {
                                Icon(
                                    imageVector = DeviceIconMapper
                                        .getIcon(
                                            type.defaultIcon,
                                        ),
                                    contentDescription = null,
                                )
                            },
                            onClick = {
                                onTypeSelected(type)
                                expanded = false
                            },
                        )
                    }
                }
            }

            // Manage Button
            OutlinedButton(
                onClick = onManageDeviceTypesClick,
                shape = RoundedCornerShape(12.dp),
                contentPadding = PaddingValues(0.dp),
                modifier = Modifier.size(56.dp),
            ) {
                Icon(Icons.Default.DevicesOther, contentDescription = "Manage Types")
            }
        }
    }
}

@Preview(showBackground = true)
@Composable
fun AddDeviceAiSectionPreview() {
    BatteryButlerTheme {
        AddDeviceAiSection(
            aiMessages = listOf(
                AiMessage("1", AiRole.USER, "Example prompt"),
                AiMessage("2", AiRole.MODEL, "Example response"),
            ),
            onBatchAdd = {},
        )
    }
}

@OptIn(ExperimentalMaterial3Api::class)
@Preview(showBackground = true)
@Composable
fun AddDeviceManualSectionPreview() {
    BatteryButlerTheme {
        AddDeviceManualSection(
            name = "My Device",
            onNameChange = {},
            location = "Kitchen",
            onLocationChange = {},
            deviceTypes = listOf(
                DeviceType("1", "Smoke Detector", "detector_smoke"),
            ),
            selectedType = DeviceType("1", "Smoke Detector", "detector_smoke"),
            onTypeSelected = {},
            onManageDeviceTypesClick = {},
        )
    }
}

@Preview(showBackground = true)
@Composable
fun AddDeviceContentPreview() {
    BatteryButlerTheme {
        AddDeviceContent(
            deviceTypes = emptyList(),
            aiMessages = emptyList(),
            onAddDevice = {},
            onBatchAdd = {},
            onManageDeviceTypesClick = {},
            onBack = {},
        )
    }
}
