package com.chriscartland.batterybutler.presentationfeature.devicetypes

import androidx.compose.foundation.ExperimentalFoundationApi
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.List
import androidx.compose.material.icons.filled.Warning
import androidx.compose.material3.Button
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.DropdownMenuItem
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import com.chriscartland.batterybutler.composeresources.composeStringResource
import com.chriscartland.batterybutler.composeresources.generated.resources.Res
import com.chriscartland.batterybutler.composeresources.generated.resources.action_load_common_types
import com.chriscartland.batterybutler.composeresources.generated.resources.empty_types_message
import com.chriscartland.batterybutler.composeresources.generated.resources.empty_types_title
import com.chriscartland.batterybutler.domain.model.DeviceType
import com.chriscartland.batterybutler.presentationcore.components.AddItemCard
import com.chriscartland.batterybutler.presentationcore.components.ButlerDropdownMenu
import com.chriscartland.batterybutler.presentationcore.components.CompositeControl
import com.chriscartland.batterybutler.presentationcore.components.DeviceTypeListItem
import com.chriscartland.batterybutler.presentationcore.components.EmptyStateContent
import com.chriscartland.batterybutler.presentationcore.theme.BatteryButlerTheme
import com.chriscartland.batterybutler.presentationcore.theme.Padding
import com.chriscartland.batterybutler.presentationfeature.util.labelRes
import com.chriscartland.batterybutler.presentationmodel.devicetypes.DeviceTypeGroupOption
import com.chriscartland.batterybutler.presentationmodel.devicetypes.DeviceTypeListUiState
import com.chriscartland.batterybutler.presentationmodel.devicetypes.DeviceTypeSortOption

@OptIn(ExperimentalFoundationApi::class)
@Composable
fun DeviceTypeListContent(
    state: DeviceTypeListUiState,
    onEditType: (String) -> Unit,
    onAddTypeClick: () -> Unit,
    onPreloadTypes: () -> Unit,
    onSortOptionSelected: (DeviceTypeSortOption) -> Unit,
    onGroupOptionSelected: (DeviceTypeGroupOption) -> Unit,
    onSortDirectionToggle: () -> Unit,
    onGroupDirectionToggle: () -> Unit,
    modifier: Modifier = Modifier,
    contentPadding: PaddingValues = PaddingValues(0.dp),
) {
    Box(modifier = modifier.fillMaxSize()) {
        when (state) {
            DeviceTypeListUiState.Loading -> {
                CircularProgressIndicator(modifier = Modifier.align(Alignment.Center))
            }
            is DeviceTypeListUiState.Error -> {
                EmptyStateContent(
                    icon = Icons.Default.Warning,
                    title = "Something went wrong",
                    message = state.message,
                    modifier = Modifier.padding(contentPadding),
                )
            }
            is DeviceTypeListUiState.Success -> {
                val allTypes = state.groupedTypes.values.flatten()
                if (allTypes.isEmpty()) {
                    EmptyStateContent(
                        icon = Icons.AutoMirrored.Filled.List,
                        title = composeStringResource(Res.string.empty_types_title),
                        message = composeStringResource(Res.string.empty_types_message),
                        modifier = Modifier.padding(contentPadding),
                        action = {
                            Column(horizontalAlignment = Alignment.CenterHorizontally) {
                                Button(onClick = onAddTypeClick) {
                                    Text("Add Type")
                                }
                                Spacer(modifier = Modifier.height(8.dp))
                                OutlinedButton(onClick = onPreloadTypes) {
                                    Text(composeStringResource(Res.string.action_load_common_types))
                                }
                            }
                        },
                    )
                } else {
                    LazyColumn(
                        modifier = Modifier.fillMaxSize(),
                        contentPadding = contentPadding,
                        verticalArrangement = Arrangement.spacedBy(Padding.medium),
                    ) {
                        item {
                            DeviceTypeFilterRow(
                                state = state,
                                onSortOptionSelected = onSortOptionSelected,
                                onGroupOptionSelected = onGroupOptionSelected,
                                onSortDirectionToggle = onSortDirectionToggle,
                                onGroupDirectionToggle = onGroupDirectionToggle,
                            )
                        }
                        item {
                            AddItemCard("Add a device type", onAddTypeClick)
                        }
                        state.groupedTypes.forEach { (groupName, types) ->
                            if (state.groupOption != DeviceTypeGroupOption.NONE) {
                                stickyHeader {
                                    Surface(
                                        modifier = Modifier.fillMaxWidth(),
                                        color = MaterialTheme.colorScheme.surfaceVariant,
                                        contentColor = MaterialTheme.colorScheme.onSurfaceVariant,
                                    ) {
                                        Text(
                                            text = groupName,
                                            modifier = Modifier.padding(horizontal = Padding.standard, vertical = Padding.small),
                                            style = MaterialTheme.typography.labelLarge,
                                            fontWeight = FontWeight.Bold,
                                        )
                                    }
                                }
                            }

                            items(types, key = { it.id }) { type ->
                                DeviceTypeListItem(
                                    deviceType = type,
                                    onClick = { onEditType(type.id) },
                                )
                            }
                        }
                    }
                }
            }
        }
    }
}

@Composable
fun DeviceTypeFilterRow(
    state: DeviceTypeListUiState.Success,
    onSortOptionSelected: (DeviceTypeSortOption) -> Unit,
    onGroupOptionSelected: (DeviceTypeGroupOption) -> Unit,
    onSortDirectionToggle: () -> Unit,
    onGroupDirectionToggle: () -> Unit,
) {
    Column {
        // Filter Row
        Row(
            modifier = Modifier
                .fillMaxWidth(),
            horizontalArrangement = Arrangement.spacedBy(Padding.small),
        ) {
            var sortExpanded by remember { mutableStateOf(false) }
            var groupExpanded by remember { mutableStateOf(false) }

            // Sort Button (First)
            Box {
                CompositeControl(
                    label = "Sort: ${composeStringResource(state.sortOption.labelRes())}",
                    isActive = true, // Sort is always active
                    isAscending = state.isSortAscending,
                    onClicked = { sortExpanded = true },
                    onDirectionToggle = { onSortDirectionToggle() },
                )
                ButlerDropdownMenu(
                    expanded = sortExpanded,
                    onDismissRequest = { sortExpanded = false },
                ) {
                    DeviceTypeSortOption.entries.forEach { option ->
                        DropdownMenuItem(
                            text = { Text(composeStringResource(option.labelRes())) },
                            onClick = {
                                onSortOptionSelected(option)
                                sortExpanded = false
                            },
                        )
                    }
                }
            }

            // Group Button (Second)
            Box {
                CompositeControl(
                    label = "Group: ${composeStringResource(state.groupOption.labelRes())}",
                    isActive = state.groupOption != DeviceTypeGroupOption.NONE,
                    isAscending = state.isGroupAscending,
                    onClicked = { groupExpanded = true },
                    onDirectionToggle = { onGroupDirectionToggle() },
                )
                ButlerDropdownMenu(
                    expanded = groupExpanded,
                    onDismissRequest = { groupExpanded = false },
                ) {
                    DeviceTypeGroupOption.entries.forEach { option ->
                        DropdownMenuItem(
                            text = { Text(composeStringResource(option.labelRes())) },
                            onClick = {
                                onGroupOptionSelected(option)
                                groupExpanded = false
                            },
                        )
                    }
                }
            }
        }
    }
}

@Preview(showBackground = true)
@Composable
fun DeviceTypeListContentEmptyPreview() {
    BatteryButlerTheme {
        DeviceTypeListContent(
            state = DeviceTypeListUiState.Success(groupedTypes = emptyMap()),
            onEditType = {},
            onAddTypeClick = {},
            onPreloadTypes = {},
            onSortOptionSelected = {},
            onGroupOptionSelected = {},
            onSortDirectionToggle = {},
            onGroupDirectionToggle = {},
        )
    }
}

@Preview(showBackground = true)
@Composable
fun DeviceTypeListContentLoadingPreview() {
    BatteryButlerTheme {
        DeviceTypeListContent(
            state = DeviceTypeListUiState.Loading,
            onEditType = {},
            onAddTypeClick = {},
            onPreloadTypes = {},
            onSortOptionSelected = {},
            onGroupOptionSelected = {},
            onSortDirectionToggle = {},
            onGroupDirectionToggle = {},
        )
    }
}

@Preview(showBackground = true)
@Composable
fun DeviceTypeListContentErrorPreview() {
    BatteryButlerTheme {
        DeviceTypeListContent(
            state = DeviceTypeListUiState.Error("Failed to load device types"),
            onEditType = {},
            onAddTypeClick = {},
            onPreloadTypes = {},
            onSortOptionSelected = {},
            onGroupOptionSelected = {},
            onSortDirectionToggle = {},
            onGroupDirectionToggle = {},
        )
    }
}

@Composable
fun DeviceTypeListContentPreview() {
    BatteryButlerTheme {
        val type = DeviceType("type1", "Smoke Alarm", "detector_smoke")
        val state = DeviceTypeListUiState.Success(
            groupedTypes = mapOf("All" to listOf(type)),
            sortOption = DeviceTypeSortOption.NAME,
            groupOption = DeviceTypeGroupOption.NONE,
            isSortAscending = true,
            isGroupAscending = true,
        )
        DeviceTypeListContent(
            state = state,
            onEditType = {},
            onAddTypeClick = {},
            onPreloadTypes = {},
            onSortOptionSelected = {},
            onGroupOptionSelected = {},
            onSortDirectionToggle = {},
            onGroupDirectionToggle = {},
        )
    }
}

@Composable
fun DeviceTypeFilterRowPreview() {
    BatteryButlerTheme {
        Surface {
            DeviceTypeFilterRow(
                state = DeviceTypeListUiState.Success(
                    groupedTypes = emptyMap(),
                    sortOption = DeviceTypeSortOption.NAME,
                    groupOption = DeviceTypeGroupOption.NONE,
                    isSortAscending = true,
                    isGroupAscending = true,
                ),
                onSortOptionSelected = {},
                onGroupOptionSelected = {},
                onSortDirectionToggle = {},
                onGroupDirectionToggle = {},
            )
        }
    }
}
