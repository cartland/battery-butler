package com.chriscartland.batterybutler.presentationfeature.home

import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.foundation.ExperimentalFoundationApi
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.ExperimentalLayoutApi
import androidx.compose.foundation.layout.FlowRow
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
import androidx.compose.material.icons.filled.DensityLarge
import androidx.compose.material.icons.filled.DensitySmall
import androidx.compose.material.icons.filled.Warning
import androidx.compose.material.icons.outlined.Devices
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.DropdownMenuItem
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Snackbar
import androidx.compose.material3.SnackbarHost
import androidx.compose.material3.SnackbarHostState
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.material3.pulltorefresh.PullToRefreshBox
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
import com.chriscartland.batterybutler.composeresources.generated.resources.action_try_again
import com.chriscartland.batterybutler.composeresources.generated.resources.add_device_title
import com.chriscartland.batterybutler.composeresources.generated.resources.add_item_card_device
import com.chriscartland.batterybutler.composeresources.generated.resources.content_desc_switch_to_compact
import com.chriscartland.batterybutler.composeresources.generated.resources.content_desc_switch_to_expanded
import com.chriscartland.batterybutler.composeresources.generated.resources.empty_devices_message
import com.chriscartland.batterybutler.composeresources.generated.resources.empty_devices_title
import com.chriscartland.batterybutler.composeresources.generated.resources.error_load_devices
import com.chriscartland.batterybutler.composeresources.generated.resources.filter_group_label
import com.chriscartland.batterybutler.composeresources.generated.resources.filter_sort_label
import com.chriscartland.batterybutler.composeresources.generated.resources.status_syncing
import com.chriscartland.batterybutler.composeresources.generated.resources.sync_error_ai
import com.chriscartland.batterybutler.composeresources.generated.resources.sync_error_auth_required
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
import com.chriscartland.batterybutler.presentationcore.components.IconControl
import com.chriscartland.batterybutler.presentationcore.theme.BatteryButlerTheme
import com.chriscartland.batterybutler.presentationcore.theme.Padding
import com.chriscartland.batterybutler.presentationfeature.util.labelRes
import com.chriscartland.batterybutler.presentationmodel.home.DensityOption
import com.chriscartland.batterybutler.presentationmodel.home.GroupOption
import com.chriscartland.batterybutler.presentationmodel.home.HomeScreenState
import com.chriscartland.batterybutler.presentationmodel.home.SortOption
import kotlin.time.Clock
import kotlin.time.ExperimentalTime
import kotlin.time.Instant

@OptIn(ExperimentalFoundationApi::class, ExperimentalTime::class, ExperimentalMaterial3Api::class)
@Composable
fun HomeScreenContent(
    state: HomeScreenState,
    onGroupOptionToggle: () -> Unit,
    onGroupOptionSelected: (GroupOption) -> Unit,
    onSortOptionToggle: () -> Unit,
    onSortOptionSelected: (SortOption) -> Unit,
    onDensityOptionSelected: (DensityOption) -> Unit,
    onDeviceClick: (Device) -> Unit,
    onAddDeviceClick: () -> Unit,
    onRetry: () -> Unit,
    modifier: Modifier = Modifier,
    contentPadding: PaddingValues = PaddingValues(0.dp),
    nowInstant: Instant = Clock.System.now(),
    isRefreshing: Boolean = false,
    onRefresh: () -> Unit = {},
) {
    val snackbarHostState = remember { SnackbarHostState() }

    // Resolve sync error message in composable scope (outside LaunchedEffect)
    val syncErrorMessage = when (val syncStatus = state.syncStatus) {
        is SyncStatus.Failed -> getSyncErrorMessage(syncStatus.error)

        // Auth-required is deliberately distinct from a generic failure: sync isn't broken,
        // the user just needs to sign in again. Covers the reactive session-expired case too
        // (a terminal 401 flips sync to AuthRequired). No tap-through affordance here by
        // design — that would thread a navigation callback through the Home scaffolding; the
        // sign-in path is the front door / Settings Labs card, which carry the cause-aware
        // "session expired" copy (see docs/LABS-AUTH.md §3).
        is SyncStatus.AuthRequired -> composeStringResource(Res.string.sync_error_auth_required)

        else -> null
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
        PullToRefreshBox(
            isRefreshing = isRefreshing,
            onRefresh = onRefresh,
            modifier = Modifier.fillMaxSize(),
        ) {
            Box(modifier = Modifier.fillMaxSize()) {
                HomeScreenList(
                    state = state,
                    onGroupOptionToggle = onGroupOptionToggle,
                    onGroupOptionSelected = onGroupOptionSelected,
                    onSortOptionToggle = onSortOptionToggle,
                    onSortOptionSelected = onSortOptionSelected,
                    onDensityOptionSelected = onDensityOptionSelected,
                    onDeviceClick = onDeviceClick,
                    onAddDeviceClick = onAddDeviceClick,
                    onRetry = onRetry,
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
                                .padding(horizontal = Padding.medium, vertical = Padding.small),
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
}

@OptIn(ExperimentalLayoutApi::class)
@Composable
fun HomeScreenFilterRow(
    state: HomeScreenState,
    onGroupOptionToggle: () -> Unit,
    onGroupOptionSelected: (GroupOption) -> Unit,
    onSortOptionToggle: () -> Unit,
    onSortOptionSelected: (SortOption) -> Unit,
    onDensityOptionSelected: (DensityOption) -> Unit,
) {
    Column {
        // FlowRow, not Row: at their longest ("Sort: Battery Age" + "Group: Location") the two
        // labelled controls plus the density icon still exceed a narrow phone's width, so they
        // wrap to a second line instead of being clipped off the right edge.
        FlowRow(
            modifier = Modifier
                .fillMaxWidth(),
            horizontalArrangement = Arrangement.spacedBy(Padding.small),
            verticalArrangement = Arrangement.spacedBy(Padding.small),
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

            // Density toggle (Third) — icon-only. A two-way choice doesn't warrant a labelled
            // dropdown, and "View: Expanded" cost more filter-row width than the control itself.
            // The icon shows the CURRENT density (DensityLarge = spaced rows, DensitySmall =
            // dense rows); the contentDescription describes what a tap does.
            val isCompact = state.densityOption == DensityOption.COMPACT
            IconControl(
                icon = if (isCompact) Icons.Default.DensitySmall else Icons.Default.DensityLarge,
                contentDescription = composeStringResource(
                    if (isCompact) {
                        Res.string.content_desc_switch_to_expanded
                    } else {
                        Res.string.content_desc_switch_to_compact
                    },
                ),
                onClicked = {
                    onDensityOptionSelected(
                        if (isCompact) DensityOption.EXPANDED else DensityOption.COMPACT,
                    )
                },
            )
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
    onDensityOptionSelected: (DensityOption) -> Unit,
    onDeviceClick: (Device) -> Unit,
    onAddDeviceClick: () -> Unit,
    onRetry: () -> Unit,
    contentPadding: androidx.compose.foundation.layout.PaddingValues,
    modifier: Modifier = Modifier,
    nowInstant: Instant = Clock.System.now(),
) {
    val allDevices = remember(state.groupedDevices) { state.groupedDevices.values.flatten() }

    val errorMessage = state.error
    if (errorMessage != null && allDevices.isEmpty()) {
        EmptyStateContent(
            icon = Icons.Default.Warning,
            title = composeStringResource(Res.string.error_load_devices),
            message = errorMessage,
            modifier = Modifier.padding(contentPadding),
            action = {
                androidx.compose.material3.Button(onClick = onRetry) {
                    Text(composeStringResource(Res.string.action_try_again))
                }
            },
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
            // Compact rows sit closer together — 12.dp gaps around 40.dp cards would give back
            // most of the height the shorter card just saved.
            verticalArrangement = Arrangement.spacedBy(
                if (state.densityOption == DensityOption.COMPACT) Padding.small else Padding.medium,
            ),
        ) {
            item {
                HomeScreenFilterRow(
                    state = state,
                    onGroupOptionToggle = onGroupOptionToggle,
                    onGroupOptionSelected = onGroupOptionSelected,
                    onSortOptionToggle = onSortOptionToggle,
                    onSortOptionSelected = onSortOptionSelected,
                    onDensityOptionSelected = onDensityOptionSelected,
                )
            }
            item {
                AddItemCard(composeStringResource(Res.string.add_item_card_device), onAddDeviceClick)
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
                        imageBytes = device.imageEtag?.let { state.deviceImagesByEtag[it] },
                        density = state.densityOption,
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
            onDensityOptionSelected = {},
            onDeviceClick = {},
            onAddDeviceClick = {},
            onRetry = {},
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
            onDensityOptionSelected = {},
            onDeviceClick = {},
            onAddDeviceClick = {},
            onRetry = {},
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
            onDensityOptionSelected = {},
            onDeviceClick = {},
            onAddDeviceClick = {},
            onRetry = {},
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
                onDensityOptionSelected = {},
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
            onDensityOptionSelected = {},
            onDeviceClick = {},
            onAddDeviceClick = {},
            onRetry = {},
            contentPadding = androidx.compose.foundation.layout
                .PaddingValues(16.dp),
            nowInstant = nowInstant,
        )
    }
}

/**
 * The whole Home screen in compact density — the wrapping filter row plus several single-line
 * device cards, which is what the density toggle is really about.
 */
@OptIn(ExperimentalTime::class, ExperimentalMaterial3Api::class, ExperimentalFoundationApi::class)
@Preview(showBackground = true)
@Composable
fun HomeScreenCompactPreview() {
    BatteryButlerTheme {
        val nowInstant = Instant.parse("2026-01-18T17:00:00Z")
        val smokeType = DeviceType("type1", "Smoke Alarm", "detector_smoke")
        val coType = DeviceType("type2", "CO Detector", "detector_co")
        val devices = listOf(
            Device("dev1", "Kitchen Smoke", "type1", Instant.parse("2026-01-13T17:00:00Z"), nowInstant, "Kitchen"),
            Device("dev2", "Hallway CO Detector", "type2", Instant.parse("2025-07-02T17:00:00Z"), nowInstant, "Hallway"),
            Device("dev3", "Bedroom Smoke", "type1", Instant.parse("2024-12-02T17:00:00Z"), nowInstant, "Bedroom"),
            Device("dev4", "Garage Smoke", "type1", Instant.parse("2025-11-20T17:00:00Z"), nowInstant, "Garage"),
        )
        HomeScreenContent(
            state = HomeScreenState(
                groupedDevices = mapOf("All Devices" to devices),
                deviceTypes = mapOf("type1" to smokeType, "type2" to coType),
                densityOption = DensityOption.COMPACT,
            ),
            onGroupOptionToggle = {},
            onGroupOptionSelected = {},
            onSortOptionToggle = {},
            onSortOptionSelected = {},
            onDensityOptionSelected = {},
            onDeviceClick = {},
            onAddDeviceClick = {},
            onRetry = {},
            nowInstant = nowInstant,
        )
    }
}

/** The same devices in expanded density, as an A/B reference against [HomeScreenCompactPreview]. */
@OptIn(ExperimentalTime::class, ExperimentalMaterial3Api::class, ExperimentalFoundationApi::class)
@Preview(showBackground = true)
@Composable
fun HomeScreenExpandedPreview() {
    BatteryButlerTheme {
        val nowInstant = Instant.parse("2026-01-18T17:00:00Z")
        val smokeType = DeviceType("type1", "Smoke Alarm", "detector_smoke")
        val coType = DeviceType("type2", "CO Detector", "detector_co")
        val devices = listOf(
            Device("dev1", "Kitchen Smoke", "type1", Instant.parse("2026-01-13T17:00:00Z"), nowInstant, "Kitchen"),
            Device("dev2", "Hallway CO Detector", "type2", Instant.parse("2025-07-02T17:00:00Z"), nowInstant, "Hallway"),
            Device("dev3", "Bedroom Smoke", "type1", Instant.parse("2024-12-02T17:00:00Z"), nowInstant, "Bedroom"),
            Device("dev4", "Garage Smoke", "type1", Instant.parse("2025-11-20T17:00:00Z"), nowInstant, "Garage"),
        )
        HomeScreenContent(
            state = HomeScreenState(
                groupedDevices = mapOf("All Devices" to devices),
                deviceTypes = mapOf("type1" to smokeType, "type2" to coType),
                densityOption = DensityOption.EXPANDED,
            ),
            onGroupOptionToggle = {},
            onGroupOptionSelected = {},
            onSortOptionToggle = {},
            onSortOptionSelected = {},
            onDensityOptionSelected = {},
            onDeviceClick = {},
            onAddDeviceClick = {},
            onRetry = {},
            nowInstant = nowInstant,
        )
    }
}
