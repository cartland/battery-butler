package com.chriscartland.batterybutler.presentationfeature.adddevice

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.text.KeyboardActions
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.DropdownMenuItem
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.ExposedDropdownMenuAnchorType
import androidx.compose.material3.ExposedDropdownMenuBox
import androidx.compose.material3.ExposedDropdownMenuDefaults
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Surface
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
import com.chriscartland.batterybutler.composeresources.composeStringResource
import com.chriscartland.batterybutler.composeresources.generated.resources.Res
import com.chriscartland.batterybutler.composeresources.generated.resources.action_add_new_device_type
import com.chriscartland.batterybutler.composeresources.generated.resources.action_cancel
import com.chriscartland.batterybutler.composeresources.generated.resources.action_save
import com.chriscartland.batterybutler.composeresources.generated.resources.add_device_manual_entry
import com.chriscartland.batterybutler.composeresources.generated.resources.add_device_title
import com.chriscartland.batterybutler.composeresources.generated.resources.label_device_name
import com.chriscartland.batterybutler.composeresources.generated.resources.label_device_type
import com.chriscartland.batterybutler.composeresources.generated.resources.label_location
import com.chriscartland.batterybutler.composeresources.generated.resources.placeholder_add_device_ai
import com.chriscartland.batterybutler.composeresources.generated.resources.status_confirmed
import com.chriscartland.batterybutler.composeresources.generated.resources.status_processing
import com.chriscartland.batterybutler.domain.model.BatchOperationResult
import com.chriscartland.batterybutler.domain.model.DeviceInput
import com.chriscartland.batterybutler.domain.model.DeviceType
import com.chriscartland.batterybutler.presentationcore.components.AiBatchImportSection
import com.chriscartland.batterybutler.presentationcore.components.ButlerCenteredTopAppBar
import com.chriscartland.batterybutler.presentationcore.components.DeviceIconMapper
import com.chriscartland.batterybutler.presentationcore.theme.BatteryButlerTheme
import com.chriscartland.batterybutler.presentationcore.theme.Padding

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun AddDeviceContent(
    deviceTypes: List<DeviceType>,
    aiMessages: List<BatchOperationResult>,
    isAiBatchImportEnabled: Boolean,
    onAddDevice: (DeviceInput) -> Unit,
    onBatchAdd: (String) -> Unit,
    onAddDeviceTypeClick: () -> Unit,
    onBack: () -> Unit,
    modifier: Modifier = Modifier,
    isLoading: Boolean = false,
    initialName: String = "",
    initialLocation: String = "",
) {
    var name by rememberSaveable { mutableStateOf(initialName) }
    var location by rememberSaveable { mutableStateOf(initialLocation) }
    var selectedType by remember { mutableStateOf<DeviceType?>(null) }
    var expanded by remember { mutableStateOf(false) }
    val focusManager = LocalFocusManager.current

    Scaffold(
        modifier = modifier,
        topBar = {
            ButlerCenteredTopAppBar(
                title = composeStringResource(Res.string.add_device_title),
                onBack = onBack,
                navigationIcon = {
                    TextButton(onClick = onBack, enabled = !isLoading) {
                        Text(composeStringResource(Res.string.action_cancel), color = MaterialTheme.colorScheme.primary)
                    }
                },
                actions = {
                    val trimmedName = name.trim()
                    val trimmedLocation = location.trim()
                    val isValid = trimmedName.isNotEmpty() && selectedType != null
                    TextButton(
                        onClick = {
                            selectedType?.let { type ->
                                if (trimmedName.isNotEmpty()) {
                                    onAddDevice(
                                        DeviceInput(
                                            name = trimmedName,
                                            location = trimmedLocation.takeIf { it.isNotEmpty() },
                                            typeId = type.id,
                                        ),
                                    )
                                }
                            }
                        },
                        enabled = !isLoading && isValid,
                    ) {
                        if (isLoading) {
                            CircularProgressIndicator(
                                modifier = Modifier.size(24.dp),
                                strokeWidth = 2.dp,
                                color = MaterialTheme.colorScheme.primary,
                            )
                        } else {
                            Text(
                                composeStringResource(Res.string.action_save),
                                color = if (isValid) {
                                    MaterialTheme.colorScheme.primary
                                } else {
                                    MaterialTheme.colorScheme.onSurface.copy(alpha = 0.38f)
                                },
                                fontWeight = FontWeight.Bold,
                            )
                        }
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
                .padding(Padding.standard),
            verticalArrangement = Arrangement.spacedBy(Padding.large),
            horizontalAlignment = Alignment.CenterHorizontally,
        ) {
            if (isAiBatchImportEnabled) {
                AddDeviceAiSection(aiMessages = aiMessages, onBatchAdd = onBatchAdd)
            }
            AddDeviceManualSection(
                name = name,
                onNameChange = { name = it },
                location = location,
                onLocationChange = { location = it },
                deviceTypes = deviceTypes,
                selectedType = selectedType,
                onTypeSelected = { selectedType = it },
                onAddDeviceTypeClick = onAddDeviceTypeClick,
                isLoading = isLoading,
            )
        }
    }
}

@Composable
fun AddDeviceAiSection(
    aiMessages: List<BatchOperationResult>,
    onBatchAdd: (String) -> Unit,
) {
    AiBatchImportSection(
        aiMessages = aiMessages,
        placeholderRes = Res.string.placeholder_add_device_ai,
        onBatchAdd = onBatchAdd,
    )
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
    onAddDeviceTypeClick: () -> Unit,
    isLoading: Boolean = false,
) {
    var expanded by remember { mutableStateOf(false) }
    val focusManager = LocalFocusManager.current

    // Manual Section
    Column(
        verticalArrangement = Arrangement.spacedBy(Padding.standard),
        modifier = Modifier.fillMaxWidth(),
    ) {
        Text(
            composeStringResource(Res.string.add_device_manual_entry),
            style = MaterialTheme.typography.titleMedium,
            color = MaterialTheme.colorScheme.primary,
        )
        OutlinedTextField(
            value = name,
            onValueChange = onNameChange,
            label = { Text(composeStringResource(Res.string.label_device_name)) },
            enabled = !isLoading,
            modifier = Modifier.fillMaxWidth(),
            shape = MaterialTheme.shapes.medium,
            keyboardOptions = KeyboardOptions(imeAction = ImeAction.Next),
            keyboardActions = KeyboardActions(
                onNext = { focusManager.moveFocus(FocusDirection.Next) },
            ),
        )

        OutlinedTextField(
            value = location,
            onValueChange = onLocationChange,
            label = { Text(composeStringResource(Res.string.label_location)) },
            enabled = !isLoading,
            modifier = Modifier.fillMaxWidth(),
            shape = MaterialTheme.shapes.medium,
            keyboardOptions = KeyboardOptions(imeAction = ImeAction.Done),
            keyboardActions = KeyboardActions(
                onDone = { focusManager.clearFocus() },
            ),
        )

        // Device Type Selection
        ExposedDropdownMenuBox(
            expanded = expanded,
            onExpandedChange = { expanded = !expanded },
        ) {
            OutlinedTextField(
                value = selectedType?.name ?: "",
                onValueChange = {},
                label = { Text(composeStringResource(Res.string.label_device_type)) },
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
                enabled = !isLoading,
                trailingIcon = { ExposedDropdownMenuDefaults.TrailingIcon(expanded = expanded) },
                modifier = Modifier
                    .menuAnchor(ExposedDropdownMenuAnchorType.PrimaryNotEditable)
                    .fillMaxWidth(),
                shape = MaterialTheme.shapes.medium,
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
                HorizontalDivider()
                DropdownMenuItem(
                    text = { Text(composeStringResource(Res.string.action_add_new_device_type), fontWeight = FontWeight.Bold) },
                    onClick = {
                        onAddDeviceTypeClick()
                        expanded = false
                    },
                )
            }
        }
    }
}

@Composable
fun AddDeviceAiSectionPreview() {
    BatteryButlerTheme {
        Surface {
            AddDeviceAiSection(
                aiMessages = listOf(
                    com.chriscartland.batterybutler.domain.model.BatchOperationResult
                        .Progress(composeStringResource(Res.string.status_processing)),
                    com.chriscartland.batterybutler.domain.model.BatchOperationResult
                        .Success(composeStringResource(Res.string.status_confirmed)),
                ),
                onBatchAdd = {},
            )
        }
    }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun AddDeviceManualSectionPreview() {
    BatteryButlerTheme {
        Surface {
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
                onAddDeviceTypeClick = {},
            )
        }
    }
}

@Preview(showBackground = true)
@Composable
fun AddDeviceContentPreview() {
    BatteryButlerTheme {
        AddDeviceContent(
            deviceTypes = emptyList(),
            aiMessages = emptyList(),
            isAiBatchImportEnabled = true,
            onAddDevice = {},
            onBatchAdd = {},
            onAddDeviceTypeClick = {},
            onBack = {},
            isLoading = false,
        )
    }
}

@Preview(showBackground = true)
@Composable
fun AddDeviceContentFilledPreview() {
    BatteryButlerTheme {
        AddDeviceContent(
            deviceTypes = listOf(
                DeviceType("1", "Smoke Detector", "detector_smoke"),
            ),
            aiMessages = emptyList(),
            isAiBatchImportEnabled = false,
            onAddDevice = {},
            onBatchAdd = {},
            onAddDeviceTypeClick = {},
            onBack = {},
            initialName = "Kitchen Smoke Detector",
            initialLocation = "Kitchen",
        )
    }
}
