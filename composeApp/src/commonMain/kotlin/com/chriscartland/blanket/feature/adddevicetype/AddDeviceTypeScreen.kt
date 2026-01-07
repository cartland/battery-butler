package com.chriscartland.blanket.feature.adddevicetype

import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.lazy.grid.GridCells
import androidx.compose.foundation.lazy.grid.LazyVerticalGrid
import androidx.compose.foundation.lazy.grid.items
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.KeyboardActions
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Add
import androidx.compose.material.icons.filled.BatteryFull
import androidx.compose.material.icons.filled.Remove
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
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
import androidx.compose.ui.draw.clip
import androidx.compose.ui.focus.FocusDirection
import androidx.compose.ui.platform.LocalFocusManager
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.ImeAction
import androidx.compose.ui.unit.dp
import com.chriscartland.blanket.domain.model.DeviceTypeInput
import com.chriscartland.blanket.ui.components.BlanketCenteredTopAppBar
import com.chriscartland.blanket.ui.components.DeviceIconMapper

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun AddDeviceTypeScreen(
    viewModel: AddDeviceTypeViewModel,
    onDeviceTypeAdded: () -> Unit,
    onBack: () -> Unit,
    modifier: Modifier = Modifier,
) {
    var name by remember { mutableStateOf("") }
    var selectedIcon by remember { mutableStateOf<String?>("videogame_asset") } // Default icon
    var batteryType by remember { mutableStateOf("AA") }
    var batteryQuantity by remember { mutableStateOf(1) }
    var batteryDropdownExpanded by remember { mutableStateOf(false) }
    val focusManager = LocalFocusManager.current

    val icons = listOf(
        "videogame_asset",
        "tv",
        "toys",
        "mouse",
        "keyboard",
        "detector_smoke",
        "flashlight_on",
        "schedule",
        "lock",
        "sensors",
        "smart_button",
        "thermostat",
        "garage_home",
    )
    val batteryTypes = listOf("AA", "AAA", "CR2032", "9V", "D", "C", "Li-ion Custom")

    Scaffold(
        modifier = modifier,
        topBar = {
            BlanketCenteredTopAppBar(
                title = "New Device Type",
                onBack = onBack,
                navigationIcon = {
                    androidx.compose.material3.TextButton(onClick = onBack) {
                        Text("Cancel", color = MaterialTheme.colorScheme.onSurfaceVariant)
                    }
                },
                actions = {
                    androidx.compose.material3.TextButton(onClick = {
                        if (name.isNotBlank()) {
                            viewModel.addDeviceType(
                                DeviceTypeInput(
                                    name = name,
                                    defaultIcon = selectedIcon,
                                    batteryType = batteryType,
                                    batteryQuantity = batteryQuantity,
                                ),
                            )
                            onDeviceTypeAdded()
                        }
                    }) {
                        Text(
                            "Save",
                            color = if (name.isNotBlank()) {
                                MaterialTheme.colorScheme.primary
                            } else {
                                MaterialTheme.colorScheme.onSurface.copy(
                                    alpha = 0.38f,
                                )
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
                .padding(horizontal = 16.dp),
            verticalArrangement = Arrangement.spacedBy(24.dp),
        ) {
            // Icon Selector
            Column(verticalArrangement = Arrangement.spacedBy(16.dp)) {
                Text("Choose an Icon", style = MaterialTheme.typography.titleMedium, fontWeight = FontWeight.Bold)
                LazyVerticalGrid(
                    columns = GridCells.Fixed(4),
                    horizontalArrangement = Arrangement.spacedBy(12.dp),
                    verticalArrangement = Arrangement.spacedBy(12.dp),
                    modifier = Modifier.height(160.dp), // Limit height
                ) {
                    items(icons) { iconName ->
                        val isSelected = selectedIcon == iconName
                        DeviceTypeIconItem(
                            iconName = iconName,
                            isSelected = isSelected,
                            onClick = { selectedIcon = iconName },
                        )
                    }
                }
            }

            androidx.compose.material3.HorizontalDivider()

            // Form Section
            Column(verticalArrangement = Arrangement.spacedBy(16.dp)) {
                Text("Device Details", style = MaterialTheme.typography.titleMedium, fontWeight = FontWeight.Bold)

                // Name
                Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                    Text("Name", style = MaterialTheme.typography.bodyMedium, fontWeight = FontWeight.Medium)
                    OutlinedTextField(
                        value = name,
                        onValueChange = { name = it },
                        placeholder = { Text("e.g. Xbox Controller") },
                        modifier = Modifier.fillMaxWidth(),
                        shape = RoundedCornerShape(12.dp),
                        keyboardOptions = KeyboardOptions(imeAction = ImeAction.Next),
                        keyboardActions = KeyboardActions(
                            onNext = { focusManager.moveFocus(FocusDirection.Next) },
                        ),
                    )
                }

                // Battery Type
                Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                    Text("Battery Type", style = MaterialTheme.typography.bodyMedium, fontWeight = FontWeight.Medium)
                    OutlinedTextField(
                        value = batteryType,
                        onValueChange = { batteryType = it },
                        label = { Text("Battery Type (e.g., AA)") },
                        modifier = Modifier.fillMaxWidth(),
                        singleLine = true,
                        shape = RoundedCornerShape(12.dp),
                        keyboardOptions = KeyboardOptions(imeAction = ImeAction.Done),
                        keyboardActions = KeyboardActions(
                            onDone = { focusManager.clearFocus() },
                        ),
                    )
                }

                // Battery Quantity
                Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                    Text("Battery Quantity", style = MaterialTheme.typography.bodyMedium, fontWeight = FontWeight.Medium)
                    Row(
                        modifier = Modifier
                            .fillMaxWidth()
                            .height(64.dp)
                            .border(1.dp, MaterialTheme.colorScheme.outline, RoundedCornerShape(12.dp))
                            .padding(horizontal = 16.dp),
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.SpaceBetween,
                    ) {
                        Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(12.dp)) {
                            Box(
                                modifier = Modifier
                                    .size(40.dp)
                                    .background(MaterialTheme.colorScheme.surfaceVariant, RoundedCornerShape(8.dp)),
                                contentAlignment = Alignment.Center,
                            ) {
                                Icon(
                                    Icons.Default.BatteryFull,
                                    contentDescription = null,
                                    tint = MaterialTheme.colorScheme.onSurfaceVariant,
                                )
                            }
                            Text(
                                "Batteries needed",
                                style = MaterialTheme.typography.bodyMedium,
                                color = MaterialTheme.colorScheme.onSurfaceVariant,
                            )
                        }

                        Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(16.dp)) {
                            IconButton(
                                onClick = { if (batteryQuantity > 1) batteryQuantity-- },
                                modifier = Modifier.background(MaterialTheme.colorScheme.surfaceVariant, RoundedCornerShape(8.dp)),
                            ) {
                                Icon(Icons.Default.Remove, contentDescription = "Decrease")
                            }
                            Text(batteryQuantity.toString(), style = MaterialTheme.typography.titleLarge, fontWeight = FontWeight.Bold)
                            IconButton(
                                onClick = { batteryQuantity++ },
                                modifier = Modifier.background(MaterialTheme.colorScheme.primary, RoundedCornerShape(8.dp)),
                            ) {
                                Icon(Icons.Default.Add, contentDescription = "Increase", tint = MaterialTheme.colorScheme.onPrimary)
                            }
                        }
                    }
                }
            }
        }
    }
}

@Composable
fun DeviceTypeIconItem(
    iconName: String,
    isSelected: Boolean,
    onClick: () -> Unit,
) {
    Column(
        horizontalAlignment = Alignment.CenterHorizontally,
        verticalArrangement = Arrangement.spacedBy(4.dp),
        modifier = Modifier.clickable { onClick() },
    ) {
        Box(
            modifier = Modifier
                .size(64.dp)
                .clip(CircleShape)
                .background(if (isSelected) MaterialTheme.colorScheme.primary else MaterialTheme.colorScheme.surfaceVariant)
                .then(if (isSelected) Modifier.border(2.dp, MaterialTheme.colorScheme.primary, CircleShape) else Modifier),
            contentAlignment = Alignment.Center,
        ) {
            Icon(
                imageVector = DeviceIconMapper.getIcon(iconName),
                contentDescription = null,
                tint = if (isSelected) MaterialTheme.colorScheme.onPrimary else MaterialTheme.colorScheme.onSurfaceVariant,
                modifier = Modifier.size(32.dp),
            )
        }
    }
}
