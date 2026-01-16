package com.chriscartland.batterybutler.presentationfeature.home

import androidx.compose.foundation.ExperimentalFoundationApi
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.material3.DropdownMenu
import androidx.compose.material3.DropdownMenuItem
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import com.chriscartland.batterybutler.presentationcore.components.CompositeControl
import com.chriscartland.batterybutler.presentationcore.components.DeviceListItem
import com.chriscartland.batterybutler.presentationfeature.util.labelRes
import com.chriscartland.batterybutler.presentationmodel.home.GroupOption
import org.jetbrains.compose.resources.stringResource
import com.chriscartland.batterybutler.presentationmodel.home.HomeUiState
import com.chriscartland.batterybutler.presentationmodel.home.SortOption

@OptIn(ExperimentalFoundationApi::class, ExperimentalMaterial3Api::class)
@Composable
fun HomeScreenContent(
    state: HomeUiState,
    onGroupOptionToggle: () -> Unit,
    onGroupOptionSelected: (GroupOption) -> Unit,
    onSortOptionToggle: () -> Unit,
    onSortOptionSelected: (SortOption) -> Unit,
    onDeviceClick: (String) -> Unit,
    modifier: Modifier = Modifier,
) {
    Scaffold(
        modifier = modifier.fillMaxSize(),
        topBar = {
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
                            label = "Group: ${stringResource(state.groupOption.labelRes())}",
                            isActive = state.groupOption != GroupOption.NONE,
                            isAscending = state.isGroupAscending,
                            onClicked = { groupExpanded = true },
                            onDirectionToggle = onGroupOptionToggle,
                        )
                        DropdownMenu(
                            expanded = groupExpanded,
                            onDismissRequest = { groupExpanded = false },
                        ) {
                            GroupOption.values().forEach { option ->
                                DropdownMenuItem(
                                    text = { Text(stringResource(option.labelRes())) },
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
                            label = "Sort: ${stringResource(state.sortOption.labelRes())}",
                            isActive = true, // Sort is always active
                            isAscending = state.isSortAscending,
                            onClicked = { sortExpanded = true },
                            onDirectionToggle = onSortOptionToggle,
                        )
                        DropdownMenu(
                            expanded = sortExpanded,
                            onDismissRequest = { sortExpanded = false },
                        ) {
                            SortOption.values().forEach { option ->
                                DropdownMenuItem(
                                    text = { Text(stringResource(option.labelRes())) },
                                    onClick = {
                                        onSortOptionSelected(option)
                                        sortExpanded = false
                                    },
                                )
                            }
                        }
                    }
                }
            }
        },
    ) { innerPadding ->
        LazyColumn(
            modifier = Modifier.fillMaxSize(),
            contentPadding = innerPadding,
        ) {
            state.groupedDevices.forEach { (groupName, devices) ->
                if (state.groupOption != GroupOption.NONE) {
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

                items(devices) { device ->
                    DeviceListItem(
                        device = device,
                        deviceType = state.deviceTypes[device.typeId],
                        onClick = { onDeviceClick(device.id) },
                    )
                }
            }
        }
    }
}
