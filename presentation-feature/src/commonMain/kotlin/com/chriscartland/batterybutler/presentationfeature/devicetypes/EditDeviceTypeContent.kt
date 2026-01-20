package com.chriscartland.batterybutler.presentationfeature.devicetypes

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.lazy.grid.GridCells
import androidx.compose.foundation.lazy.grid.LazyVerticalGrid
import androidx.compose.foundation.lazy.grid.items
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.KeyboardActions
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Add
import androidx.compose.material.icons.filled.Remove
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
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
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.focus.FocusDirection
import androidx.compose.ui.platform.LocalFocusManager
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.ImeAction
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import com.chriscartland.batterybutler.domain.model.DeviceType
import com.chriscartland.batterybutler.domain.model.DeviceTypeInput
import com.chriscartland.batterybutler.presentationcore.components.ButlerCenteredTopAppBar
import com.chriscartland.batterybutler.presentationcore.components.DeviceIconMapper
import com.chriscartland.batterybutler.presentationcore.components.DeviceTypeIconItem
import com.chriscartland.batterybutler.presentationcore.theme.BatteryButlerTheme
import com.chriscartland.batterybutler.presentationmodel.devicetypes.EditDeviceTypeUiState

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun EditDeviceTypeContent(
    uiState: EditDeviceTypeUiState,
    onSave: (DeviceTypeInput) -> Unit,
    onDelete: () -> Unit,
    onBack: () -> Unit,
    modifier: Modifier = Modifier,
) {
    var name by remember { mutableStateOf("") }
    var batteryType by remember { mutableStateOf("") }
    var batteryQuantity by remember { mutableStateOf(1) }
    var defaultIcon by remember { mutableStateOf("devices_other") }
    var isInitialized by remember { mutableStateOf(false) }
    var showDeleteDialog by remember { mutableStateOf(false) }
    val focusManager = LocalFocusManager.current

    Scaffold(
        modifier = modifier,
        topBar = {
            ButlerCenteredTopAppBar(
                title = "Edit Device Type",
                onBack = onBack,
                navigationIcon = {
                    TextButton(onClick = onBack) { Text("Cancel") }
                },
                actions = {
                    TextButton(
                        onClick = {
                            onSave(DeviceTypeInput(name, defaultIcon, batteryType, batteryQuantity))
                        },
                        enabled = name.isNotBlank() && batteryType.isNotBlank(),
                    ) {
                        Text("Save", fontWeight = FontWeight.Bold)
                    }
                },
            )
        },
    ) { innerPadding ->
        Box(modifier = Modifier.fillMaxSize().padding(innerPadding)) {
            when (uiState) {
                EditDeviceTypeUiState.Loading -> CircularProgressIndicator(modifier = Modifier.align(Alignment.Center))
                EditDeviceTypeUiState.NotFound -> Text("Device Type not found", modifier = Modifier.align(Alignment.Center))
                is EditDeviceTypeUiState.Success -> {
                    val original = uiState.deviceType
                    LaunchedEffect(original) {
                        if (!isInitialized) {
                            name = original.name
                            batteryType = original.batteryType
                            batteryQuantity = original.batteryQuantity
                            defaultIcon = original.defaultIcon ?: "devices_other"
                            isInitialized = true
                        }
                    }

                    Column(
                        modifier = Modifier
                            .fillMaxSize()
                            .padding(16.dp),
                    ) {
                        OutlinedTextField(
                            value = name,
                            onValueChange = { name = it },
                            label = { Text("Type Name") },
                            modifier = Modifier.fillMaxWidth(),
                            singleLine = true,
                            keyboardOptions = KeyboardOptions(imeAction = ImeAction.Next),
                            keyboardActions = KeyboardActions(onNext = { focusManager.moveFocus(FocusDirection.Next) }),
                        )
                        Spacer(modifier = Modifier.height(16.dp))
                        OutlinedTextField(
                            value = batteryType,
                            onValueChange = { batteryType = it },
                            label = { Text("Battery Type (e.g., AA)") },
                            modifier = Modifier.fillMaxWidth(),
                            singleLine = true,
                            keyboardOptions = KeyboardOptions(imeAction = ImeAction.Done),
                            keyboardActions = KeyboardActions(onDone = { focusManager.clearFocus() }),
                        )
                        Spacer(modifier = Modifier.height(16.dp))

                        Row(verticalAlignment = Alignment.CenterVertically, modifier = Modifier.fillMaxWidth()) {
                            Text("Quantity", style = MaterialTheme.typography.bodyLarge)
                            Spacer(Modifier.weight(1f))
                            IconButton(onClick = { if (batteryQuantity > 1) batteryQuantity-- }) {
                                Icon(Icons.Default.Remove, contentDescription = "Decrease")
                            }
                            Text(
                                batteryQuantity.toString(),
                                style = MaterialTheme.typography.titleMedium,
                                modifier = Modifier.padding(horizontal = 8.dp),
                            )
                            IconButton(onClick = { batteryQuantity++ }) {
                                Icon(Icons.Default.Add, contentDescription = "Increase")
                            }
                        }

                        Spacer(modifier = Modifier.height(24.dp))

                        // Icon Selection
                        Text("Icon", style = MaterialTheme.typography.labelLarge)
                        val icons = remember(uiState.usedIcons) {
                            DeviceIconMapper.AvailableIcons.sortedByDescending { it in uiState.usedIcons }
                        }
                        LazyVerticalGrid(
                            columns = GridCells.Fixed(4),
                            horizontalArrangement = Arrangement.spacedBy(12.dp),
                            verticalArrangement = Arrangement.spacedBy(12.dp),
                            modifier = Modifier.weight(1f).padding(vertical = 8.dp),
                        ) {
                            items(icons) { iconName ->
                                val isSelected = defaultIcon == iconName
                                DeviceTypeIconItem(
                                    iconName = iconName,
                                    isSelected = isSelected,
                                    onClick = { defaultIcon = iconName },
                                )
                            }
                        }

                        Spacer(modifier = Modifier.height(16.dp))

                        Button(
                            onClick = { showDeleteDialog = true },
                            colors = ButtonDefaults.buttonColors(
                                containerColor = MaterialTheme.colorScheme.errorContainer,
                                contentColor = MaterialTheme.colorScheme.error,
                            ),
                            modifier = Modifier.fillMaxWidth().height(56.dp),
                            shape = RoundedCornerShape(12.dp),
                        ) {
                            Text("Delete Device Type", fontWeight = FontWeight.SemiBold)
                        }
                    }
                }
            }
        }
        if (showDeleteDialog) {
            AlertDialog(
                onDismissRequest = { showDeleteDialog = false },
                title = { Text("Delete Device Type") },
                text = { Text("Are you sure you want to delete this device type?") },
                confirmButton = {
                    TextButton(onClick = {
                        onDelete()
                        showDeleteDialog = false
                    }, colors = ButtonDefaults.textButtonColors(contentColor = MaterialTheme.colorScheme.error)) { Text("Delete") }
                },
                dismissButton = { TextButton(onClick = { showDeleteDialog = false }) { Text("Cancel") } },
            )
        }
    }
}

@Preview(showBackground = true)
@Composable
fun EditDeviceTypeContentPreview() {
    BatteryButlerTheme {
        val type = DeviceType("type1", "Smoke Alarm", "detector_smoke")
        EditDeviceTypeContent(
            uiState = EditDeviceTypeUiState.Success(type, usedIcons = emptyList()),
            onSave = {},
            onDelete = {},
            onBack = {},
        )
    }
}
