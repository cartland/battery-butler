package com.chriscartland.batterybutler.presentationfeature.devicetypes

import androidx.compose.foundation.ExperimentalFoundationApi
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.DropdownMenu
import androidx.compose.material3.DropdownMenuItem
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Icon
import androidx.compose.material3.ListItem
import androidx.compose.material3.MaterialTheme
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
import com.chriscartland.batterybutler.domain.model.DeviceType
import com.chriscartland.batterybutler.presentationcore.components.CompositeControl
import com.chriscartland.batterybutler.presentationcore.components.DeviceIconMapper
import com.chriscartland.batterybutler.composeresources.composeStringResource
import com.chriscartland.batterybutler.presentationcore.theme.BatteryButlerTheme
import com.chriscartland.batterybutler.presentationfeature.util.labelRes
import com.chriscartland.batterybutler.presentationmodel.devicetypes.DeviceTypeGroupOption
import com.chriscartland.batterybutler.presentationmodel.devicetypes.DeviceTypeListUiState
import com.chriscartland.batterybutler.presentationmodel.devicetypes.DeviceTypeSortOption

@OptIn(ExperimentalMaterial3Api::class, ExperimentalFoundationApi::class)
@Composable
fun DeviceTypeListContent(
    state: DeviceTypeListUiState,
    onEditType: (String) -> Unit,
    onSortOptionSelected: (DeviceTypeSortOption) -> Unit,
    onGroupOptionSelected: (DeviceTypeGroupOption) -> Unit,
    onSortDirectionToggle: () -> Unit,
    onGroupDirectionToggle: () -> Unit,
    modifier: Modifier = Modifier,
) {
    Box(modifier = modifier.fillMaxSize()) {
        when (state) {
            DeviceTypeListUiState.Loading -> {
                CircularProgressIndicator(modifier = Modifier.align(Alignment.Center))
            }
            is DeviceTypeListUiState.Success -> {
                Column {
                    // Filter Row
                    Row(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(horizontal = 16.dp, vertical = 8.dp),
                        horizontalArrangement = Arrangement.spacedBy(8.dp),
                    ) {
                        var sortExpanded by remember { mutableStateOf(false) }
                        var groupExpanded by remember { mutableStateOf(false) }

                        // Group Button (First)
                        Box {
                            CompositeControl(
                                label = "Group: ${composeStringResource(state.groupOption.labelRes())}",
                                isActive = state.groupOption != DeviceTypeGroupOption.NONE,
                                isAscending = state.isGroupAscending,
                                onClicked = { groupExpanded = true },
                                onDirectionToggle = { onGroupDirectionToggle() },
                            )
                            DropdownMenu(
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

                        // Sort Button (Second)
                        Box {
                            CompositeControl(
                                label = "Sort: ${composeStringResource(state.sortOption.labelRes())}",
                                isActive = true, // Sort is always active
                                isAscending = state.isSortAscending,
                                onClicked = { sortExpanded = true },
                                onDirectionToggle = { onSortDirectionToggle() },
                            )
                            DropdownMenu(
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
                    }

                    LazyColumn(
                        modifier = Modifier.fillMaxSize(),
                        contentPadding = PaddingValues(16.dp),
                    ) {
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
                                            modifier = Modifier.padding(horizontal = 16.dp, vertical = 8.dp),
                                            style = MaterialTheme.typography.labelLarge,
                                            fontWeight = FontWeight.Bold,
                                        )
                                    }
                                }
                            }

                            items(types) { type ->
                                ListItem(
                                    headlineContent = { Text(type.name, fontWeight = FontWeight.Medium) },
                                    supportingContent = { Text("${type.batteryQuantity} x ${type.batteryType}") },
                                    leadingContent = {
                                        Icon(
                                            imageVector = DeviceIconMapper.getIcon(type.defaultIcon),
                                            contentDescription = null,
                                            tint = MaterialTheme.colorScheme.primary,
                                        )
                                    },
                                    modifier = Modifier.clickable { onEditType(type.id) },
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
            onSortOptionSelected = {},
            onGroupOptionSelected = {},
            onSortDirectionToggle = {},
            onGroupDirectionToggle = {},
        )
    }
}
