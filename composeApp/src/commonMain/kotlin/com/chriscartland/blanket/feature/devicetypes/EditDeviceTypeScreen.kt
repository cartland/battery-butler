package com.chriscartland.blanket.feature.devicetypes

import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Add
import androidx.compose.material.icons.filled.Remove
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
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import com.chriscartland.blanket.ui.components.DeviceIconMapper

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun EditDeviceTypeScreen(
    viewModel: EditDeviceTypeViewModel,
    onBack: () -> Unit,
    onDelete: () -> Unit,
    modifier: Modifier = Modifier,
) {
    val uiState by viewModel.uiState.collectAsState()

    var name by remember { mutableStateOf("") }
    var batteryType by remember { mutableStateOf("") }
    var batteryQuantity by remember { mutableStateOf(1) }
    var defaultIcon by remember { mutableStateOf("devices_other") }
    var isInitialized by remember { mutableStateOf(false) }

    Scaffold(
        modifier = modifier,
        topBar = {
            com.chriscartland.blanket.ui.components.BlanketCenteredTopAppBar(
                title = "Edit Device Type",
                onBack = onBack,
                navigationIcon = {
                    TextButton(onClick = onBack) {
                        Text("Cancel")
                    }
                },
                actions = {
                    TextButton(
                        onClick = {
                            viewModel.updateDeviceType(name, batteryType, batteryQuantity, defaultIcon)
                            onBack()
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
            when (val state = uiState) {
                EditDeviceTypeUiState.Loading -> {
                    CircularProgressIndicator(modifier = Modifier.align(Alignment.Center))
                }
                EditDeviceTypeUiState.NotFound -> {
                    Text("Device Type not found", modifier = Modifier.align(Alignment.Center))
                }
                is EditDeviceTypeUiState.Success -> {
                    val original = state.deviceType
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
                        // Name
                        OutlinedTextField(
                            value = name,
                            onValueChange = { name = it },
                            label = { Text("Type Name") },
                            modifier = Modifier.fillMaxWidth(),
                            singleLine = true,
                        )

                        Spacer(modifier = Modifier.height(16.dp))

                        // Battery Type
                        OutlinedTextField(
                            value = batteryType,
                            onValueChange = { batteryType = it },
                            label = { Text("Battery Type (e.g., AA)") },
                            modifier = Modifier.fillMaxWidth(),
                            singleLine = true,
                        )

                        Spacer(modifier = Modifier.height(16.dp))

                        // Quantity
                        Row(
                            verticalAlignment = Alignment.CenterVertically,
                            modifier = Modifier.fillMaxWidth(),
                        ) {
                            Text("Quantity", style = MaterialTheme.typography.bodyLarge)
                            Spacer(Modifier.weight(1f))
                            IconButton(onClick = { if (batteryQuantity > 1) batteryQuantity-- }) {
                                Icon(Icons.Default.Remove, contentDescription = "Decrease")
                            }
                            Text(
                                text = batteryQuantity.toString(),
                                style = MaterialTheme.typography.titleMedium,
                                modifier = Modifier.padding(horizontal = 8.dp),
                            )
                            IconButton(onClick = { batteryQuantity++ }) {
                                Icon(Icons.Default.Add, contentDescription = "Increase")
                            }
                        }

                        Spacer(modifier = Modifier.height(24.dp))

                        // Icon Selection (Mini)
                        Text("Default Icon", style = MaterialTheme.typography.labelLarge)
                        val icons = listOf(
                            "settings_remote",
                            "scale",
                            "schedule",
                            "flashlight_on",
                            "detector_smoke",
                            "toys",
                            "videogame_asset",
                            "devices_other",
                        )
                        Row(modifier = Modifier.fillMaxWidth()) {
                            // Simple horizontal scroll or just first few
                            icons.take(5).forEach { iconName ->
                                IconButton(onClick = { defaultIcon = iconName }) {
                                    Icon(
                                        imageVector = DeviceIconMapper.getIcon(iconName),
                                        contentDescription = null,
                                        tint = if (defaultIcon ==
                                            iconName
                                        ) {
                                            MaterialTheme.colorScheme.primary
                                        } else {
                                            MaterialTheme.colorScheme.onSurfaceVariant
                                        },
                                    )
                                }
                            }
                        }

                        Spacer(modifier = Modifier.weight(1f))

                        Button(
                            onClick = {
                                viewModel.deleteDeviceType()
                                onDelete()
                            },
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
    }
}
