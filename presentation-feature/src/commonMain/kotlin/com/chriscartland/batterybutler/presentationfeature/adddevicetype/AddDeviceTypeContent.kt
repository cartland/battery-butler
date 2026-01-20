package com.chriscartland.batterybutler.presentationfeature.adddevicetype

import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.grid.GridCells
import androidx.compose.foundation.lazy.grid.LazyVerticalGrid
import androidx.compose.foundation.lazy.grid.items
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.KeyboardActions
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Add
import androidx.compose.material.icons.filled.AutoAwesome
import androidx.compose.material.icons.filled.BatteryFull
import androidx.compose.material.icons.filled.Remove
import androidx.compose.material3.ExperimentalMaterial3Api
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
import androidx.compose.ui.focus.FocusDirection
import androidx.compose.ui.platform.LocalFocusManager
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.ImeAction
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import com.chriscartland.batterybutler.domain.ai.AiMessage
import com.chriscartland.batterybutler.domain.ai.AiRole
import com.chriscartland.batterybutler.domain.model.DeviceTypeInput
import com.chriscartland.batterybutler.presentationcore.components.ButlerCenteredTopAppBar
import com.chriscartland.batterybutler.presentationcore.components.DeviceIconMapper
import com.chriscartland.batterybutler.presentationcore.components.DeviceTypeIconItem
import com.chriscartland.batterybutler.presentationcore.theme.BatteryButlerTheme

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun AddDeviceTypeContent(
    aiMessages: List<AiMessage>,
    suggestedIcon: String? = null,
    onSuggestIcon: (String) -> Unit = {},
    onConsumeSuggestedIcon: () -> Unit = {},
    onDeviceTypeAdded: (DeviceTypeInput) -> Unit,
    onBatchAdd: (String) -> Unit,
    onBack: () -> Unit,
    modifier: Modifier = Modifier,
) {
    var name by remember { mutableStateOf("") }
    var selectedIcon by remember { mutableStateOf<String?>("videogame_asset") }
    var batteryType by remember { mutableStateOf("AA") }
    var batteryQuantity by remember { mutableStateOf(1) }
    val focusManager = LocalFocusManager.current

    androidx.compose.runtime.LaunchedEffect(suggestedIcon) {
        suggestedIcon?.let {
            selectedIcon = it
            onConsumeSuggestedIcon()
        }
    }

    val icons = DeviceIconMapper.AvailableIcons

    Scaffold(
        modifier = modifier,
        topBar = {
            ButlerCenteredTopAppBar(
                title = "New Device Type",
                onBack = onBack,
                navigationIcon = {
                    TextButton(onClick = onBack) {
                        Text("Cancel", color = MaterialTheme.colorScheme.onSurfaceVariant)
                    }
                },
                actions = {
                    TextButton(onClick = {
                        if (name.isNotBlank()) {
                            onDeviceTypeAdded(
                                DeviceTypeInput(
                                    name = name,
                                    defaultIcon = selectedIcon,
                                    batteryType = batteryType,
                                    batteryQuantity = batteryQuantity,
                                ),
                            )
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
                .verticalScroll(rememberScrollState())
                .padding(horizontal = 16.dp),
            verticalArrangement = Arrangement.spacedBy(24.dp),
        ) {
            // AI Section
            Column(modifier = Modifier.fillMaxWidth()) {
                Text(
                    "Batch Import (AI)",
                    style = MaterialTheme.typography.titleMedium,
                    color = MaterialTheme.colorScheme.primary,
                    modifier = Modifier.padding(bottom = 8.dp),
                )

                var aiInput by remember { mutableStateOf("") }
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    verticalAlignment = Alignment.CenterVertically,
                ) {
                    OutlinedTextField(
                        value = aiInput,
                        onValueChange = { aiInput = it },
                        modifier = Modifier.weight(1f),
                        placeholder = { Text("E.g. Add AA, AAA, and 9V types") },
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

            Column(verticalArrangement = Arrangement.spacedBy(16.dp)) {
                Text("Choose an Icon", style = MaterialTheme.typography.titleMedium, fontWeight = FontWeight.Bold)
                LazyVerticalGrid(
                    columns = GridCells.Fixed(4),
                    horizontalArrangement = Arrangement.spacedBy(12.dp),
                    verticalArrangement = Arrangement.spacedBy(12.dp),
                    modifier = Modifier.height(160.dp),
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

            HorizontalDivider()

            Column(verticalArrangement = Arrangement.spacedBy(16.dp)) {
                Text("Device Details", style = MaterialTheme.typography.titleMedium, fontWeight = FontWeight.Bold)

                Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                    Text("Name", style = MaterialTheme.typography.bodyMedium, fontWeight = FontWeight.Medium)
                    OutlinedTextField(
                        value = name,
                        onValueChange = { name = it },
                        placeholder = { Text("e.g. Xbox Controller") },
                        modifier = Modifier.fillMaxWidth(),
                        shape = RoundedCornerShape(12.dp),
                        keyboardOptions = KeyboardOptions(imeAction = ImeAction.Next),
                        keyboardActions = KeyboardActions(onNext = { focusManager.moveFocus(FocusDirection.Next) }),
                        trailingIcon = {
                            if (name.isNotBlank()) {
                                IconButton(onClick = { onSuggestIcon(name) }) {
                                    Icon(
                                        Icons.Default.AutoAwesome,
                                        contentDescription = "Suggest Icon",
                                        tint = MaterialTheme.colorScheme.primary,
                                    )
                                }
                            }
                        },
                    )
                }

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
                        keyboardActions = KeyboardActions(onDone = { focusManager.clearFocus() }),
                    )
                }

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

@Preview(showBackground = true)
@Composable
fun AddDeviceTypeContentPreview() {
    BatteryButlerTheme {
        AddDeviceTypeContent(
            aiMessages = listOf(
                AiMessage("1", AiRole.USER, "Add Smoke Detector"),
                AiMessage("2", AiRole.MODEL, "Confirmed."),
            ),
            suggestedIcon = "detector_smoke",
            onDeviceTypeAdded = {},
            onBatchAdd = {},
            onBack = {},
        )
    }
}
