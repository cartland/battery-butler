package com.chriscartland.batterybutler.presentationfeature.home

import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.expandVertically
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.animation.shrinkVertically
import androidx.compose.foundation.ExperimentalFoundationApi
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.calculateEndPadding
import androidx.compose.foundation.layout.calculateStartPadding
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.outlined.Devices
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.DropdownMenu
import androidx.compose.material3.DropdownMenuItem
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Snackbar
import androidx.compose.material3.SnackbarHost
import androidx.compose.material3.SnackbarHostState
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalLayoutDirection
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import com.chriscartland.batterybutler.composeresources.composeStringResource
import com.chriscartland.batterybutler.composeresources.generated.resources.Res
import com.chriscartland.batterybutler.composeresources.generated.resources.empty_devices_message
import com.chriscartland.batterybutler.composeresources.generated.resources.empty_devices_title
import com.chriscartland.batterybutler.domain.model.Device
import com.chriscartland.batterybutler.domain.model.DeviceType
import com.chriscartland.batterybutler.domain.model.SyncStatus
import com.chriscartland.batterybutler.presentationcore.components.CompositeControl
import com.chriscartland.batterybutler.presentationcore.components.DeviceListItem
import com.chriscartland.batterybutler.presentationcore.components.EmptyStateContent
import com.chriscartland.batterybutler.presentationcore.theme.BatteryButlerTheme
import com.chriscartland.batterybutler.presentationfeature.util.labelRes
import com.chriscartland.batterybutler.presentationmodel.home.GroupOption
import com.chriscartland.batterybutler.presentationmodel.home.HomeUiState
import com.chriscartland.batterybutler.presentationmodel.home.SortOption
import kotlin.time.Clock
import kotlin.time.ExperimentalTime
import kotlin.time.Instant

@OptIn(ExperimentalFoundationApi::class, ExperimentalMaterial3Api::class, ExperimentalTime::class)
@Composable
fun HomeScreenContent(
    state: HomeUiState,
    onGroupOptionToggle: () -> Unit,
    onGroupOptionSelected: (GroupOption) -> Unit,
    onSortOptionToggle: () -> Unit,
    onSortOptionSelected: (SortOption) -> Unit,
    onDeviceClick: (String) -> Unit,
    modifier: Modifier = Modifier,
    contentPadding: PaddingValues = PaddingValues(0.dp),
    nowInstant: Instant = Clock.System.now(),
) {
    val snackbarHostState = remember { SnackbarHostState() }

    // Show snackbar when sync fails
    LaunchedEffect(state.syncStatus) {
        when (val status = state.syncStatus) {
            is SyncStatus.Failed -> {
                snackbarHostState.showSnackbar(
                    message = "Sync failed: ${status.error.message}",
                )
            }
            else -> {}
        }
    }

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
        snackbarHost = {
            SnackbarHost(snackbarHostState) { data ->
                Snackbar(
                    snackbarData = data,
                    containerColor = MaterialTheme.colorScheme.errorContainer,
                    contentColor = MaterialTheme.colorScheme.onErrorContainer,
                )
            }
        },
    ) { innerPadding ->
        val layoutDirection = LocalLayoutDirection.current
        val mergedPadding = PaddingValues(
            top = innerPadding.calculateTopPadding() + contentPadding.calculateTopPadding(),
            bottom = innerPadding.calculateBottomPadding() + contentPadding.calculateBottomPadding(),
            start = innerPadding.calculateStartPadding(layoutDirection) + contentPadding.calculateStartPadding(layoutDirection),
            end = innerPadding.calculateEndPadding(layoutDirection) + contentPadding.calculateEndPadding(layoutDirection),
        )
        HomeScreenList(
            state = state,
            onDeviceClick = onDeviceClick,
            contentPadding = mergedPadding,
            nowInstant = nowInstant,
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
        // Sync status indicator with smooth animation
        AnimatedVisibility(
            visible = state.syncStatus is SyncStatus.Syncing,
            enter = expandVertically() + fadeIn(),
            exit = shrinkVertically() + fadeOut(),
        ) {
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(horizontal = 16.dp, vertical = 4.dp),
                horizontalArrangement = Arrangement.spacedBy(8.dp),
            ) {
                CircularProgressIndicator(
                    modifier = Modifier.size(16.dp),
                    strokeWidth = 2.dp,
                )
                Text(
                    text = "Syncing...",
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.primary,
                )
            }
        }

        // Filter Row
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = 16.dp, vertical = 8.dp),
            horizontalArrangement = Arrangement.spacedBy(8.dp),
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
                    onDirectionToggle = onSortOptionToggle,
                )
                DropdownMenu(
                    expanded = sortExpanded,
                    onDismissRequest = { sortExpanded = false },
                ) {
                    SortOption.entries.forEach { option ->
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
                    isActive = state.groupOption != GroupOption.NONE,
                    isAscending = state.isGroupAscending,
                    onClicked = { groupExpanded = true },
                    onDirectionToggle = onGroupOptionToggle,
                )
                DropdownMenu(
                    expanded = groupExpanded,
                    onDismissRequest = { groupExpanded = false },
                ) {
                    GroupOption.entries.forEach { option ->
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

@OptIn(ExperimentalFoundationApi::class, ExperimentalTime::class)
@Composable
fun HomeScreenList(
    state: HomeUiState,
    onDeviceClick: (String) -> Unit,
    contentPadding: androidx.compose.foundation.layout.PaddingValues,
    nowInstant: Instant = Clock.System.now(),
) {
    val allDevices = state.groupedDevices.values.flatten()

    if (allDevices.isEmpty()) {
        EmptyStateContent(
            icon = Icons.Outlined.Devices,
            title = composeStringResource(Res.string.empty_devices_title),
            message = composeStringResource(Res.string.empty_devices_message),
            modifier = Modifier.padding(contentPadding),
        )
    } else {
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

                items(devices, key = { it.id }) { device ->
                    DeviceListItem(
                        device = device,
                        deviceType = state.deviceTypes[device.typeId],
                        onClick = { onDeviceClick(device.id) },
                        nowInstant = nowInstant,
                    )
                }
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
            nowInstant = now,
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

@OptIn(ExperimentalTime::class)
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
            nowInstant = now,
        )
    }
}
