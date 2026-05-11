package com.chriscartland.batterybutler.presentationfeature.home

import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.foundation.ExperimentalFoundationApi
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.WindowInsets
import androidx.compose.foundation.layout.calculateEndPadding
import androidx.compose.foundation.layout.calculateStartPadding
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Warning
import androidx.compose.material.icons.outlined.Devices
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.DropdownMenuItem
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
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalLayoutDirection
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import com.chriscartland.batterybutler.composeresources.composeStringResource
import com.chriscartland.batterybutler.composeresources.generated.resources.Res
import com.chriscartland.batterybutler.composeresources.generated.resources.add_device_title
import com.chriscartland.batterybutler.composeresources.generated.resources.empty_devices_message
import com.chriscartland.batterybutler.composeresources.generated.resources.empty_devices_title
import com.chriscartland.batterybutler.composeresources.generated.resources.error_something_went_wrong
import com.chriscartland.batterybutler.composeresources.generated.resources.filter_group_label
import com.chriscartland.batterybutler.composeresources.generated.resources.filter_sort_label
import com.chriscartland.batterybutler.composeresources.generated.resources.status_syncing
import com.chriscartland.batterybutler.composeresources.generated.resources.sync_error_ai
import com.chriscartland.batterybutler.composeresources.generated.resources.sync_error_data
import com.chriscartland.batterybutler.composeresources.generated.resources.sync_error_network
import com.chriscartland.batterybutler.composeresources.generated.resources.sync_error_not_ready
import com.chriscartland.batterybutler.composeresources.generated.resources.sync_error_server
import com.chriscartland.batterybutler.composeresources.generated.resources.sync_error_sync_failed
import com.chriscartland.batterybutler.composeresources.generated.resources.sync_error_timeout
import com.chriscartland.batterybutler.composeresources.generated.resources.sync_error_unknown
import com.chriscartland.batterybutler.domain.model.DataError
import com.chriscartland.batterybutler.domain.model.Device
import com.chriscartland.batterybutler.domain.model.DeviceType
import com.chriscartland.batterybutler.domain.model.SyncStatus
import com.chriscartland.batterybutler.presentationcore.components.AddItemCard
import com.chriscartland.batterybutler.presentationcore.components.ButlerDropdownMenu
import com.chriscartland.batterybutler.presentationcore.components.CompositeControl
import com.chriscartland.batterybutler.presentationcore.components.DeviceListItem
import com.chriscartland.batterybutler.presentationcore.components.EmptyStateContent
import com.chriscartland.batterybutler.presentationcore.theme.BatteryButlerTheme
import com.chriscartland.batterybutler.presentationcore.theme.Padding
import com.chriscartland.batterybutler.presentationfeature.util.labelRes
import com.chriscartland.batterybutler.presentationmodel.home.GroupOption
import com.chriscartland.batterybutler.presentationmodel.home.HomeScreenState
import com.chriscartland.batterybutler.presentationmodel.home.SortOption
import kotlin.time.Clock
import kotlin.time.ExperimentalTime
import kotlin.time.Instant

@OptIn(ExperimentalFoundationApi::class, ExperimentalTime::class)
@Composable
fun HomeScreenContent(
    state: HomeScreenState,
    onGroupOptionToggle: () -> Unit,
    onGroupOptionSelected: (GroupOption) -> Unit,
    onSortOptionToggle: () -> Unit,
    onSortOptionSelected: (SortOption) -> Unit,
    onDeviceClick: (Device) -> Unit,
    onAddDeviceClick: () -> Unit,
    modifier: Modifier = Modifier,
    contentPadding: PaddingValues = PaddingValues(0.dp),
    nowInstant: Instant = Clock.System.now(),
) {
    val snackbarHostState = remember { SnackbarHostState() }

    // Resolve sync error message in composable scope (outside LaunchedEffect)
    val syncErrorMessage = (state.syncStatus as? SyncStatus.Failed)?.let {
        getSyncErrorMessage(it.error)
    }

    // Show snackbar when sync fails
    LaunchedEffect(state.syncStatus) {
        syncErrorMessage?.let {
            snackbarHostState.showSnackbar(message = it)
        }
    }

    Scaffold(
        modifier = modifier.fillMaxSize(),
        // NavDisplay entries don't propagate LocalConsumedWindowInsets from the outer Scaffold.
        // The outer MainScreenShell Scaffold + contentModifier already position this composable
        // below the top bar, so this inner Scaffold must not re-apply window insets.
        contentWindowInsets = WindowInsets(0, 0, 0, 0),
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
        Box(modifier = Modifier.fillMaxSize()) {
            HomeScreenList(
                state = state,
                onGroupOptionToggle = onGroupOptionToggle,
                onGroupOptionSelected = onGroupOptionSelected,
                onSortOptionToggle = onSortOptionToggle,
                onSortOptionSelected = onSortOptionSelected,
                onDeviceClick = onDeviceClick,
                onAddDeviceClick = onAddDeviceClick,
                contentPadding = mergedPadding,
                nowInstant = nowInstant,
            )

            // Sync status indicator overlay
            AnimatedVisibility(
                visible = state.syncStatus is SyncStatus.Syncing,
                enter = fadeIn(),
                exit = fadeOut(),
                modifier = Modifier
                    .align(Alignment.TopCenter)
                    .padding(top = mergedPadding.calculateTopPadding() + 8.dp),
            ) {
                Surface(
                    shape = MaterialTheme.shapes.small,
                    color = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.9f),
                    shadowElevation = 4.dp,
                    tonalElevation = 4.dp,
                ) {
                    Row(
                        modifier = Modifier
                            .padding(horizontal = 12.dp, vertical = 8.dp),
                        horizontalArrangement = Arrangement.spacedBy(8.dp),
                        verticalAlignment = Alignment.CenterVertically,
                    ) {
                        CircularProgressIndicator(
                            modifier = Modifier.size(12.dp),
                            strokeWidth = 2.dp,
                        )
                        Text(
                            text = composeStringResource(Res.string.status_syncing),
                            style = MaterialTheme.typography.labelMedium,
                            color = MaterialTheme.colorScheme.onSurfaceVariant,
                        )
                    }
                }
            }
        }
    }
}

@Composable
fun HomeScreenFilterRow(
    state: HomeScreenState,
    onGroupOptionToggle: () -> Unit,
    onGroupOptionSelected: (GroupOption) -> Unit,
    onSortOptionToggle: () -> Unit,
    onSortOptionSelected: (SortOption) -> Unit,
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
                    label = composeStringResource(
                        Res.string.filter_sort_label,
                        composeStringResource(state.sortOption.labelRes()),
                    ),
                    isActive = true, // Sort is always active
                    isAscending = state.isSortAscending,
                    onClicked = { sortExpanded = true },
                    onDirectionToggle = onSortOptionToggle,
                )
                ButlerDropdownMenu(
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
                    label = composeStringResource(
                        Res.string.filter_group_label,
                        composeStringResource(state.groupOption.labelRes()),
                    ),
                    isActive = state.groupOption != GroupOption.NONE,
                    isAscending = state.isGroupAscending,
                    onClicked = { groupExpanded = true },
                    onDirectionToggle = onGroupOptionToggle,
                )
                ButlerDropdownMenu(
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
    state: HomeScreenState,
    onGroupOptionToggle: () -> Unit,
    onGroupOptionSelected: (GroupOption) -> Unit,
    onSortOptionToggle: () -> Unit,
    onSortOptionSelected: (SortOption) -> Unit,
    onDeviceClick: (Device) -> Unit,
    onAddDeviceClick: () -> Unit,
    contentPadding: androidx.compose.foundation.layout.PaddingValues,
    modifier: Modifier = Modifier,
    nowInstant: Instant = Clock.System.now(),
) {
    val allDevices = state.groupedDevices.values.flatten()

    val errorMessage = state.error
    if (errorMessage != null && allDevices.isEmpty()) {
        EmptyStateContent(
            icon = Icons.Default.Warning,
            title = composeStringResource(Res.string.error_something_went_wrong),
            message = errorMessage,
            modifier = Modifier.padding(contentPadding),
        )
    } else if (allDevices.isEmpty()) {
        EmptyStateContent(
            icon = Icons.Outlined.Devices,
            title = composeStringResource(Res.string.empty_devices_title),
            message = composeStringResource(Res.string.empty_devices_message),
            modifier = Modifier.padding(contentPadding),
            action = {
                androidx.compose.material3.Button(onClick = onAddDeviceClick) {
                    Text(composeStringResource(Res.string.add_device_title))
                }
            },
        )
    } else {
        LazyColumn(
            modifier = modifier.fillMaxSize(),
            contentPadding = contentPadding,
            verticalArrangement = Arrangement.spacedBy(Padding.medium),
        ) {
            item {
                HomeScreenFilterRow(
                    state = state,
                    onGroupOptionToggle = onGroupOptionToggle,
                    onGroupOptionSelected = onGroupOptionSelected,
                    onSortOptionToggle = onSortOptionToggle,
                    onSortOptionSelected = onSortOptionSelected,
                )
            }
            item {
                AddItemCard("Add a device", onAddDeviceClick)
            }
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
                                modifier = Modifier.padding(horizontal = Padding.standard, vertical = Padding.small),
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
                        onClick = { onDeviceClick(device) },
                        nowInstant = nowInstant,
                    )
                }
            }
        }
    }
}

/**
 * Returns a user-friendly error message for sync failures.
 */
@OptIn(ExperimentalTime::class)
@Composable
fun getSyncErrorMessage(error: DataError): String =
    when (error) {
        is DataError.Network.ConnectionFailed -> {
            composeStringResource(Res.string.sync_error_network)
        }

        is DataError.Network.Timeout -> {
            composeStringResource(Res.string.sync_error_timeout)
        }

        is DataError.Network.ServerError -> {
            composeStringResource(Res.string.sync_error_server)
        }

        is DataError.Network.NotReady -> {
            composeStringResource(Res.string.sync_error_not_ready)
        }

        is DataError.Network.PushFailed -> {
            composeStringResource(Res.string.sync_error_sync_failed)
        }

        is DataError.Database.ReadFailed,
        is DataError.Database.WriteFailed,
        is DataError.Database.ConstraintViolation,
        -> {
            composeStringResource(Res.string.sync_error_data)
        }

        is DataError.Ai.ApiError,
        is DataError.Ai.ParsingError,
        -> {
            composeStringResource(Res.string.sync_error_ai)
        }

        is DataError.Unknown -> {
            composeStringResource(Res.string.sync_error_unknown)
        }
    }

@Preview(showBackground = true)
@Composable
fun HomeScreenPreview() {
    BatteryButlerTheme {
        // Use fixed dates for stable screenshots
        val nowInstant = Instant.parse("2026-01-18T17:00:00Z")
        val batteryReplacedInstant = Instant.parse("2026-01-13T17:00:00Z") // 5 days ago
        val type = DeviceType("type1", "Smoke Alarm", "detector_smoke")
        val device = Device("dev1", "Kitchen Smoke", "type1", batteryReplacedInstant, nowInstant, "Kitchen")
        val state = HomeScreenState(
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
            onAddDeviceClick = {},
            nowInstant = nowInstant,
        )
    }
}

@OptIn(ExperimentalTime::class)
@Preview(showBackground = true)
@Composable
fun HomeScreenErrorPreview() {
    BatteryButlerTheme {
        HomeScreenContent(
            state = HomeScreenState(error = "Failed to load devices"),
            onGroupOptionToggle = {},
            onGroupOptionSelected = {},
            onSortOptionToggle = {},
            onSortOptionSelected = {},
            onDeviceClick = {},
            onAddDeviceClick = {},
            nowInstant = Instant.parse("2026-01-18T17:00:00Z"),
        )
    }
}

@OptIn(ExperimentalTime::class)
@Preview(showBackground = true)
@Composable
fun HomeScreenEmptyPreview() {
    BatteryButlerTheme {
        HomeScreenContent(
            state = HomeScreenState(groupedDevices = emptyMap(), deviceTypes = emptyMap()),
            onGroupOptionToggle = {},
            onGroupOptionSelected = {},
            onSortOptionToggle = {},
            onSortOptionSelected = {},
            onDeviceClick = {},
            onAddDeviceClick = {},
            nowInstant = Instant.parse("2026-01-18T17:00:00Z"),
        )
    }
}

@Composable
fun HomeScreenFilterRowPreview() {
    BatteryButlerTheme {
        Surface {
            HomeScreenFilterRow(
                state = HomeScreenState(
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
}

@OptIn(ExperimentalTime::class)
@Composable
fun HomeScreenListPreview() {
    BatteryButlerTheme {
        // Use fixed dates for stable screenshots
        val nowInstant = Instant.parse("2026-01-18T17:00:00Z")
        val batteryReplacedInstant = Instant.parse("2026-01-13T17:00:00Z") // 5 days ago
        val type = DeviceType("type1", "Smoke Alarm", "detector_smoke")
        val device = Device("dev1", "Kitchen Smoke", "type1", batteryReplacedInstant, nowInstant, "Kitchen")
        HomeScreenList(
            state = HomeScreenState(
                groupedDevices = mapOf("All" to listOf(device)),
                deviceTypes = mapOf("type1" to type),
            ),
            onGroupOptionToggle = {},
            onGroupOptionSelected = {},
            onSortOptionToggle = {},
            onSortOptionSelected = {},
            onDeviceClick = {},
            onAddDeviceClick = {},
            contentPadding = androidx.compose.foundation.layout
                .PaddingValues(16.dp),
            nowInstant = nowInstant,
        )
    }
}
