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
import com.chriscartland.batterybutler.composeresources.generated.resources.action_cancel
import com.chriscartland.batterybutler.composeresources.generated.resources.action_decrease
import com.chriscartland.batterybutler.composeresources.generated.resources.action_increase
import com.chriscartland.batterybutler.composeresources.generated.resources.action_save
import com.chriscartland.batterybutler.composeresources.generated.resources.add_device_type_title
import com.chriscartland.batterybutler.composeresources.generated.resources.content_desc_batteries_needed
import com.chriscartland.batterybutler.composeresources.generated.resources.content_desc_suggest_icon
import com.chriscartland.batterybutler.composeresources.generated.resources.label_battery_quantity
import com.chriscartland.batterybutler.composeresources.generated.resources.label_battery_type
import com.chriscartland.batterybutler.composeresources.generated.resources.label_battery_type_hint
import com.chriscartland.batterybutler.composeresources.generated.resources.label_device_details
import com.chriscartland.batterybutler.composeresources.generated.resources.label_icon
import com.chriscartland.batterybutler.composeresources.generated.resources.label_name
import com.chriscartland.batterybutler.composeresources.generated.resources.placeholder_batch_import
import com.chriscartland.batterybutler.composeresources.generated.resources.placeholder_device_name
import com.chriscartland.batterybutler.domain.model.BatchOperationResult
import com.chriscartland.batterybutler.domain.model.DeviceTypeInput
import com.chriscartland.batterybutler.presentationcore.components.AiBatchImportSection
import com.chriscartland.batterybutler.presentationcore.components.ButlerCenteredTopAppBar
import com.chriscartland.batterybutler.presentationcore.components.DeviceIconMapper
import com.chriscartland.batterybutler.presentationcore.components.DeviceTypeIconItem
import com.chriscartland.batterybutler.presentationcore.theme.BatteryButlerTheme
import com.chriscartland.batterybutler.presentationmodel.adddevicetype.AddDeviceTypeUiState

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun AddDeviceTypeContent(
    uiState: AddDeviceTypeUiState,
    onSuggestIcon: (String) -> Unit = {},
    onConsumeSuggestedIcon: () -> Unit = {},
    onDeviceTypeAdded: (DeviceTypeInput) -> Unit,
    onBatchAdd: (String) -> Unit,
    onBack: () -> Unit,
    modifier: Modifier = Modifier,
) {
    var name by rememberSaveable { mutableStateOf("") }
    var selectedIcon by rememberSaveable { mutableStateOf<String?>("videogame_asset") }
    var batteryType by rememberSaveable { mutableStateOf("AA") }
    var batteryQuantity by rememberSaveable { mutableStateOf(1) }
    val focusManager = LocalFocusManager.current

    androidx.compose.runtime.LaunchedEffect(uiState.suggestedIcon) {
        uiState.suggestedIcon?.let {
            selectedIcon = it
            onConsumeSuggestedIcon()
        }
    }

    val icons = remember(uiState.usedIcons) {
        DeviceIconMapper.AvailableIcons.sortedByDescending { it in uiState.usedIcons }
    }

    Scaffold(
        modifier = modifier,
        topBar = {
            ButlerCenteredTopAppBar(
                title = composeStringResource(Res.string.add_device_type_title),
                onBack = onBack,
                navigationIcon = {
                    TextButton(onClick = onBack) {
                        Text(composeStringResource(Res.string.action_cancel), color = MaterialTheme.colorScheme.onSurfaceVariant)
                    }
                },
                actions = {
                    val isValid = name.isNotBlank() && batteryType.isNotBlank()
                    TextButton(onClick = {
                        if (isValid) {
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
                            composeStringResource(Res.string.action_save),
                            color = if (isValid) {
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
            // AI Section (only shown when AI is available)
            if (uiState.isAiBatchImportEnabled) {
                AiBatchImportSection(
                    aiMessages = uiState.aiMessages,
                    placeholderRes = Res.string.placeholder_batch_import,
                    onBatchAdd = onBatchAdd,
                )
            }

            Column(verticalArrangement = Arrangement.spacedBy(16.dp)) {
                Text(composeStringResource(Res.string.label_icon), style = MaterialTheme.typography.titleMedium, fontWeight = FontWeight.Bold)
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
                Text(
                    composeStringResource(Res.string.label_device_details),
                    style = MaterialTheme.typography.titleMedium,
                    fontWeight = FontWeight.Bold,
                )

                Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                    Text(composeStringResource(Res.string.label_name), style = MaterialTheme.typography.bodyMedium, fontWeight = FontWeight.Medium)
                    OutlinedTextField(
                        value = name,
                        onValueChange = { name = it },
                        placeholder = { Text(composeStringResource(Res.string.placeholder_device_name)) },
                        modifier = Modifier.fillMaxWidth(),
                        shape = RoundedCornerShape(12.dp),
                        keyboardOptions = KeyboardOptions(imeAction = ImeAction.Next),
                        keyboardActions = KeyboardActions(onNext = { focusManager.moveFocus(FocusDirection.Next) }),
                        trailingIcon = {
                            if (name.isNotBlank()) {
                                if (uiState.isSuggestingIcon) {
                                    androidx.compose.material3.CircularProgressIndicator(
                                        modifier = Modifier.size(24.dp),
                                        strokeWidth = 2.dp,
                                    )
                                } else {
                                    IconButton(onClick = { onSuggestIcon(name) }) {
                                        Icon(
                                            Icons.Default.AutoAwesome,
                                            contentDescription = composeStringResource(Res.string.content_desc_suggest_icon),
                                            tint = MaterialTheme.colorScheme.primary,
                                        )
                                    }
                                }
                            }
                        },
                    )
                }

                Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                    Text(
                        composeStringResource(Res.string.label_battery_type),
                        style = MaterialTheme.typography.bodyMedium,
                        fontWeight = FontWeight.Medium,
                    )
                    OutlinedTextField(
                        value = batteryType,
                        onValueChange = { batteryType = it },
                        label = { Text(composeStringResource(Res.string.label_battery_type_hint)) },
                        modifier = Modifier.fillMaxWidth(),
                        singleLine = true,
                        shape = RoundedCornerShape(12.dp),
                        keyboardOptions = KeyboardOptions(imeAction = ImeAction.Done),
                        keyboardActions = KeyboardActions(onDone = { focusManager.clearFocus() }),
                    )
                }

                Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                    Text(
                        composeStringResource(Res.string.label_battery_quantity),
                        style = MaterialTheme.typography.bodyMedium,
                        fontWeight = FontWeight.Medium,
                    )
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
                                composeStringResource(Res.string.content_desc_batteries_needed),
                                style = MaterialTheme.typography.bodyMedium,
                                color = MaterialTheme.colorScheme.onSurfaceVariant,
                            )
                        }
                        Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(16.dp)) {
                            IconButton(
                                onClick = { if (batteryQuantity > 1) batteryQuantity-- },
                                modifier = Modifier.background(MaterialTheme.colorScheme.surfaceVariant, RoundedCornerShape(8.dp)),
                            ) {
                                Icon(Icons.Default.Remove, contentDescription = composeStringResource(Res.string.action_decrease))
                            }
                            Text(batteryQuantity.toString(), style = MaterialTheme.typography.titleLarge, fontWeight = FontWeight.Bold)
                            IconButton(
                                onClick = { batteryQuantity++ },
                                modifier = Modifier.background(MaterialTheme.colorScheme.primary, RoundedCornerShape(8.dp)),
                            ) {
                                Icon(
                                    Icons.Default.Add,
                                    contentDescription = composeStringResource(Res.string.action_increase),
                                    tint = MaterialTheme.colorScheme.onPrimary,
                                )
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
            uiState = AddDeviceTypeUiState(
                aiMessages = listOf(
                    BatchOperationResult.Progress("Processing..."),
                    BatchOperationResult.Success("Confirmed"),
                ),
                isAiBatchImportEnabled = true,
                suggestedIcon = "detector_smoke",
            ),
            onDeviceTypeAdded = {},
            onBatchAdd = {},
            onBack = {},
        )
    }
}
