package com.chriscartland.batterybutler.feature.home

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
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Download
import androidx.compose.material3.DropdownMenu
import androidx.compose.material3.DropdownMenuItem
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import com.chriscartland.batterybutler.ui.components.ButlerCenteredTopAppBar
import com.chriscartland.batterybutler.ui.components.CompositeControl
import com.chriscartland.batterybutler.ui.components.DeviceListItem
import com.chriscartland.batterybutler.ui.util.LocalShareHandler

@OptIn(ExperimentalMaterial3Api::class, ExperimentalFoundationApi::class)
@Composable
fun HomeScreen(
    viewModel: HomeViewModel,
    onAddDeviceClick: () -> Unit,
    onDeviceClick: (String) -> Unit,
    onManageTypesClick: () -> Unit,
    modifier: Modifier = Modifier,
) {
    val state by viewModel.uiState.collectAsState()

    val coreUiState = state

    val shareHandler = LocalShareHandler.current
    // Handle Export Data
    androidx.compose.runtime.LaunchedEffect(coreUiState.exportData) {
        coreUiState.exportData?.let { data ->
            shareHandler.shareText(data)
            viewModel.onExportDataConsumed()
        }
    }

    Scaffold(
        modifier = modifier.fillMaxSize(),
        topBar = {
            Column {
                // ButlerCenteredTopAppBar(
                //     title = "Battery Butler",
                //     actions = {
                //         IconButton(onClick = { viewModel.onExportData() }) {
                //             Icon(
                //                 imageVector = Icons.Default.Download,
                //                 contentDescription = "Export Data",
                //             )
                //         }
                //     },
                // )
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
                            label = "Group: ${coreUiState.groupOption.label}",
                            isActive = coreUiState.groupOption != GroupOption.NONE,
                            isAscending = coreUiState.isGroupAscending,
                            onClicked = { groupExpanded = true },
                            onDirectionToggle = { viewModel.toggleGroupDirection() },
                        )
                        DropdownMenu(
                            expanded = groupExpanded,
                            onDismissRequest = { groupExpanded = false },
                        ) {
                            GroupOption.values().forEach { option ->
                                DropdownMenuItem(
                                    text = { Text(option.label) },
                                    onClick = {
                                        viewModel.onGroupOptionSelected(option)
                                        groupExpanded = false
                                    },
                                )
                            }
                        }
                    }

                    // Sort Button (Second)
                    Box {
                        CompositeControl(
                            label = "Sort: ${coreUiState.sortOption.label}",
                            isActive = true, // Sort is always active
                            isAscending = coreUiState.isSortAscending,
                            onClicked = { sortExpanded = true },
                            onDirectionToggle = { viewModel.toggleSortDirection() },
                        )
                        DropdownMenu(
                            expanded = sortExpanded,
                            onDismissRequest = { sortExpanded = false },
                        ) {
                            SortOption.values().forEach { option ->
                                DropdownMenuItem(
                                    text = { Text(option.label) },
                                    onClick = {
                                        viewModel.onSortOptionSelected(option)
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
            modifier = Modifier.fillMaxSize().padding(innerPadding),
        ) {
            coreUiState.groupedDevices.forEach { (groupName, devices) ->
                if (coreUiState.groupOption != GroupOption.NONE) {
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
                        deviceType = coreUiState.deviceTypes[device.typeId],
                        onClick = { onDeviceClick(device.id) },
                    )
                }
            }
        }
    }
}
