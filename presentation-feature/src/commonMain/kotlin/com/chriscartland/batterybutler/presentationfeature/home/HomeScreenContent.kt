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
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import com.chriscartland.batterybutler.composeresources.composeStringResource
import com.chriscartland.batterybutler.domain.model.Device
import com.chriscartland.batterybutler.domain.model.DeviceType
import com.chriscartland.batterybutler.presentationcore.components.CompositeControl
import com.chriscartland.batterybutler.presentationcore.components.DeviceListItem
import com.chriscartland.batterybutler.presentationcore.theme.BatteryButlerTheme
import com.chriscartland.batterybutler.presentationfeature.util.labelRes
import com.chriscartland.batterybutler.presentationmodel.home.GroupOption
import com.chriscartland.batterybutler.presentationmodel.home.HomeUiState
import com.chriscartland.batterybutler.presentationmodel.home.SortOption
import kotlin.time.ExperimentalTime
import kotlin.time.Instant

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
            HomeScreenFilterRow(
                state = state,
                onGroupOptionToggle = onGroupOptionToggle,
                onGroupOptionSelected = onGroupOptionSelected,
                onSortOptionToggle = onSortOptionToggle,
                onSortOptionSelected = onSortOptionSelected,
            )
        },
    ) { innerPadding ->
        HomeScreenList(
            state = state,
            onDeviceClick = onDeviceClick,
            contentPadding = innerPadding,
        )
    }
}

@Composable
fun HomeScreenFilterRow(
    state: HomeUiState,
    onGroupOptionToggle: () -> Unit,
    onGroupOptionSelected: (GroupOption) -> Unit,
    onSortOptionToggle: () -> Unit,
    onSortOptionSelected: (SortOption) -> Unit,
) {
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
                    onDirectionToggle = onSortOptionToggle,
                )
                DropdownMenu(
                    expanded = sortExpanded,
                    onDismissRequest = { sortExpanded = false },
                ) {
                    SortOption.values().forEach { option ->
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
    }
}

@OptIn(ExperimentalFoundationApi::class)
@Composable
fun HomeScreenList(
    state: HomeUiState,
    onDeviceClick: (String) -> Unit,
    contentPadding: androidx.compose.foundation.layout.PaddingValues,
) {
    LazyColumn(
        modifier = Modifier.fillMaxSize(),
        contentPadding = contentPadding,
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

@OptIn(ExperimentalTime::class)
@Preview(showBackground = true)
@Composable
fun HomeScreenPreview() {
    BatteryButlerTheme {
        val now = Instant.parse("2026-01-18T17:00:00Z")
        val type = DeviceType("type1", "Smoke Alarm", "detector_smoke")
        val device = Device("dev1", "Kitchen Smoke", "type1", now, now, "Kitchen")
        val state = HomeUiState(
            groupedDevices = mapOf("All" to listOf(device)),
            deviceTypes = mapOf("type1" to type),
        )
        HomeScreenContent(
            state = state,
            onGroupOptionToggle = {},
            onGroupOptionSelected = {},
            onSortOptionToggle = {},
            onSortOptionSelected = {},
            onDeviceClick = {},
        )
    }
}

@Preview(showBackground = true)
@Composable
fun HomeScreenFilterRowPreview() {
    BatteryButlerTheme {
        HomeScreenFilterRow(
            state = HomeUiState(
                groupedDevices = emptyMap(),
                deviceTypes = emptyMap(),
            ),
            onGroupOptionToggle = {},
            onGroupOptionSelected = {},
            onSortOptionToggle = {},
            onSortOptionSelected = {},
        )
    }
}

@Preview(showBackground = true)
@Composable
fun HomeScreenListPreview() {
    BatteryButlerTheme {
        val now = Instant.parse("2026-01-18T17:00:00Z")
        val type = DeviceType("type1", "Smoke Alarm", "detector_smoke")
        val device = Device("dev1", "Kitchen Smoke", "type1", now, now, "Kitchen")
        HomeScreenList(
            state = HomeUiState(
                groupedDevices = mapOf("All" to listOf(device)),
                deviceTypes = mapOf("type1" to type),
            ),
            onDeviceClick = {},
            contentPadding = androidx.compose.foundation.layout
                .PaddingValues(16.dp),
        )
    }
}
