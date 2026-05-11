package com.chriscartland.batterybutler.presentationfeature.editdevice

import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.KeyboardActions
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
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
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
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
import com.chriscartland.batterybutler.composeresources.generated.resources.action_delete
import com.chriscartland.batterybutler.composeresources.generated.resources.action_delete_device
import com.chriscartland.batterybutler.composeresources.generated.resources.action_save_bold
import com.chriscartland.batterybutler.composeresources.generated.resources.dialog_delete_device_text
import com.chriscartland.batterybutler.composeresources.generated.resources.dialog_delete_device_title
import com.chriscartland.batterybutler.composeresources.generated.resources.edit_device_title
import com.chriscartland.batterybutler.composeresources.generated.resources.error_device_not_found
import com.chriscartland.batterybutler.composeresources.generated.resources.label_device_name
import com.chriscartland.batterybutler.composeresources.generated.resources.label_device_type
import com.chriscartland.batterybutler.composeresources.generated.resources.label_location
import com.chriscartland.batterybutler.composeresources.generated.resources.label_select_type
import com.chriscartland.batterybutler.domain.model.Device
import com.chriscartland.batterybutler.domain.model.DeviceInput
import com.chriscartland.batterybutler.domain.model.DeviceType
import com.chriscartland.batterybutler.presentationcore.components.ButlerCenteredTopAppBar
import com.chriscartland.batterybutler.presentationcore.components.DeviceIconMapper
import com.chriscartland.batterybutler.presentationcore.theme.BatteryButlerTheme
import com.chriscartland.batterybutler.presentationcore.theme.Padding
import com.chriscartland.batterybutler.presentationmodel.editdevice.EditDeviceScreenState
import kotlin.time.ExperimentalTime
import kotlin.time.Instant

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun EditDeviceContent(
    uiState: EditDeviceScreenState,
    onSave: (DeviceInput) -> Unit,
    onDelete: () -> Unit,
    onAddDeviceTypeClick: () -> Unit,
    onBack: () -> Unit,
    modifier: Modifier = Modifier,
) {
    // Local state for form fields
    var name by rememberSaveable { mutableStateOf("") }
    var location by rememberSaveable { mutableStateOf("") }
    var selectedTypeId by rememberSaveable { mutableStateOf("") }
    var isInitialized by rememberSaveable { mutableStateOf(false) }
    var showDeleteDialog by rememberSaveable { mutableStateOf(false) }
    val focusManager = LocalFocusManager.current

    Scaffold(
        modifier = modifier,
        topBar = {
            ButlerCenteredTopAppBar(
                title = composeStringResource(Res.string.edit_device_title),
                onBack = onBack,
                navigationIcon = {
                    TextButton(onClick = onBack) {
                        Text(composeStringResource(Res.string.action_cancel))
                    }
                },
                actions = {
                    TextButton(
                        onClick = {
                            onSave(
                                DeviceInput(
                                    name = name,
                                    location = location.takeIf { it.isNotBlank() },
                                    typeId = selectedTypeId,
                                ),
                            )
                        },
                        enabled = name.isNotBlank() && selectedTypeId.isNotBlank(),
                    ) {
                        Text(composeStringResource(Res.string.action_save_bold), fontWeight = FontWeight.Bold)
                    }
                },
            )
        },
    ) { innerPadding ->
        Box(modifier = Modifier.fillMaxSize().padding(innerPadding)) {
            when (val state = uiState) {
                EditDeviceScreenState.Loading -> {
                    CircularProgressIndicator(modifier = Modifier.align(Alignment.Center))
                }

                EditDeviceScreenState.NotFound -> {
                    Text(composeStringResource(Res.string.error_device_not_found), modifier = Modifier.align(Alignment.Center))
                }

                is EditDeviceScreenState.Success -> {
                    // Initialize fields once
                    LaunchedEffect(state) {
                        if (!isInitialized) {
                            name = state.device.name
                            location = state.device.location ?: ""
                            selectedTypeId = state.device.typeId
                            isInitialized = true
                        }
                    }

                    Column(
                        modifier = Modifier
                            .fillMaxSize()
                            .verticalScroll(rememberScrollState())
                            .padding(Padding.standard),
                    ) {
                        // Name Input
                        OutlinedTextField(
                            value = name,
                            onValueChange = { name = it },
                            label = { Text(composeStringResource(Res.string.label_device_name)) },
                            modifier = Modifier.fillMaxWidth(),
                            singleLine = true,
                            keyboardOptions = KeyboardOptions(imeAction = ImeAction.Next),
                            keyboardActions = KeyboardActions(
                                onNext = { focusManager.moveFocus(FocusDirection.Next) },
                            ),
                        )

                        Spacer(modifier = Modifier.height(16.dp))

                        OutlinedTextField(
                            value = location,
                            onValueChange = { location = it },
                            label = { Text(composeStringResource(Res.string.label_location)) },
                            modifier = Modifier.fillMaxWidth(),
                            singleLine = true,
                            keyboardOptions = KeyboardOptions(imeAction = ImeAction.Done),
                            keyboardActions = KeyboardActions(
                                onDone = { focusManager.clearFocus() },
                            ),
                        )

                        Spacer(modifier = Modifier.height(16.dp))

                        // Type Dropdown
                        val selectedType = state.deviceTypes.find { it.id == selectedTypeId }
                        var expanded by remember { mutableStateOf(false) }

                        ExposedDropdownMenuBox(
                            expanded = expanded,
                            onExpandedChange = { expanded = !expanded },
                        ) {
                            OutlinedTextField(
                                modifier = Modifier
                                    .menuAnchor(
                                        ExposedDropdownMenuAnchorType.PrimaryNotEditable,
                                        enabled = true,
                                    ).fillMaxWidth(),
                                readOnly = true,
                                value = selectedType?.name ?: composeStringResource(Res.string.label_select_type),
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
                                trailingIcon = { ExposedDropdownMenuDefaults.TrailingIcon(expanded = expanded) },
                                colors = ExposedDropdownMenuDefaults.outlinedTextFieldColors(),
                            )
                            ExposedDropdownMenu(
                                expanded = expanded,
                                onDismissRequest = { expanded = false },
                            ) {
                                state.deviceTypes.forEach { type ->
                                    DropdownMenuItem(
                                        text = { Text(type.name) },
                                        leadingIcon = {
                                            Icon(
                                                imageVector = DeviceIconMapper.getIcon(
                                                    type.defaultIcon,
                                                ),
                                                contentDescription = null,
                                            )
                                        },
                                        onClick = {
                                            selectedTypeId = type.id
                                            expanded = false
                                        },
                                        contentPadding = ExposedDropdownMenuDefaults.ItemContentPadding,
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

                        Spacer(modifier = Modifier.height(24.dp))

                        // Delete Button
                        Button(
                            onClick = {
                                showDeleteDialog = true
                            },
                            colors = ButtonDefaults.buttonColors(
                                containerColor = MaterialTheme.colorScheme.errorContainer,
                                contentColor = MaterialTheme.colorScheme.onErrorContainer,
                            ),
                            modifier = Modifier.fillMaxWidth().height(56.dp),
                            shape = RoundedCornerShape(12.dp),
                        ) {
                            Text(composeStringResource(Res.string.action_delete_device), fontWeight = FontWeight.SemiBold)
                        }
                    }
                }
            }
        }

        if (showDeleteDialog) {
            AlertDialog(
                onDismissRequest = { showDeleteDialog = false },
                title = { Text(composeStringResource(Res.string.dialog_delete_device_title)) },
                text = { Text(composeStringResource(Res.string.dialog_delete_device_text)) },
                confirmButton = {
                    TextButton(
                        onClick = {
                            onDelete()
                            showDeleteDialog = false
                        },
                        colors = ButtonDefaults.textButtonColors(contentColor = MaterialTheme.colorScheme.error),
                    ) {
                        Text(composeStringResource(Res.string.action_delete))
                    }
                },
                dismissButton = {
                    TextButton(onClick = { showDeleteDialog = false }) {
                        Text(composeStringResource(Res.string.action_cancel))
                    }
                },
            )
        }
    }
}

@Preview(showBackground = true)
@Composable
fun EditDeviceLoadingPreview() {
    BatteryButlerTheme {
        EditDeviceContent(
            uiState = EditDeviceScreenState.Loading,
            onSave = {},
            onDelete = {},
            onAddDeviceTypeClick = {},
            onBack = {},
        )
    }
}

@Preview(showBackground = true)
@Composable
fun EditDeviceNotFoundPreview() {
    BatteryButlerTheme {
        EditDeviceContent(
            uiState = EditDeviceScreenState.NotFound,
            onSave = {},
            onDelete = {},
            onAddDeviceTypeClick = {},
            onBack = {},
        )
    }
}

@OptIn(ExperimentalTime::class)
@Preview(showBackground = true)
@Composable
fun EditDeviceContentPreview() {
    BatteryButlerTheme {
        val now = Instant.parse("2026-01-18T17:00:00Z")
        val type = DeviceType("type1", "Smoke Alarm", "detector_smoke")
        val device = Device("dev1", "Kitchen Smoke", "type1", now, now, "Kitchen")
        EditDeviceContent(
            uiState = EditDeviceScreenState.Success(
                device = device,
                deviceTypes = listOf(type),
            ),
            onSave = {},
            onDelete = {},
            onAddDeviceTypeClick = {},
            onBack = {},
        )
    }
}
