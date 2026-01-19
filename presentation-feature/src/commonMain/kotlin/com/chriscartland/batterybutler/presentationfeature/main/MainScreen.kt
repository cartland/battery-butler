package com.chriscartland.batterybutler.presentationfeature.main

import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.padding
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.List
import androidx.compose.material.icons.filled.Add
import androidx.compose.material.icons.filled.History
import androidx.compose.material.icons.filled.Home
import androidx.compose.material.icons.filled.Settings
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.FloatingActionButton
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.NavigationBar
import androidx.compose.material3.NavigationBarItem
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.tooling.preview.Preview
import batterybutler.presentation_feature.generated.resources.Res
import batterybutler.presentation_feature.generated.resources.tab_devices
import batterybutler.presentation_feature.generated.resources.tab_history
import batterybutler.presentation_feature.generated.resources.tab_types
import com.chriscartland.batterybutler.domain.model.BatteryEvent
import com.chriscartland.batterybutler.domain.model.Device
import com.chriscartland.batterybutler.domain.model.DeviceType
import com.chriscartland.batterybutler.presentationcore.components.ButlerCenteredTopAppBar
import com.chriscartland.batterybutler.presentationcore.theme.BatteryButlerTheme
import com.chriscartland.batterybutler.presentationfeature.devicetypes.DeviceTypeListContent
import com.chriscartland.batterybutler.presentationfeature.history.HistoryListContent
import com.chriscartland.batterybutler.presentationfeature.home.HomeScreenContent
import com.chriscartland.batterybutler.presentationmodel.devicetypes.DeviceTypeGroupOption
import com.chriscartland.batterybutler.presentationmodel.devicetypes.DeviceTypeListUiState
import com.chriscartland.batterybutler.presentationmodel.devicetypes.DeviceTypeSortOption
import com.chriscartland.batterybutler.presentationmodel.history.HistoryItemUiModel
import com.chriscartland.batterybutler.presentationmodel.history.HistoryListUiState
import com.chriscartland.batterybutler.presentationmodel.home.GroupOption
import com.chriscartland.batterybutler.presentationmodel.home.HomeUiState
import com.chriscartland.batterybutler.presentationmodel.home.SortOption
import kotlinx.serialization.Serializable
import org.jetbrains.compose.resources.StringResource
import org.jetbrains.compose.resources.stringResource
import kotlin.time.Clock
import kotlin.time.ExperimentalTime
import kotlin.time.Instant

@Serializable
enum class MainTab {
    Devices,
    Types,
    History,
}

fun MainTab.labelRes(): StringResource =
    when (this) {
        MainTab.Devices -> Res.string.tab_devices
        MainTab.Types -> Res.string.tab_types
        MainTab.History -> Res.string.tab_history
    }

fun MainTab.icon(): ImageVector =
    when (this) {
        MainTab.Devices -> Icons.Default.Home
        MainTab.Types -> Icons.AutoMirrored.Filled.List
        MainTab.History -> Icons.Default.History
    }

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun MainScreenShell(
    currentTab: MainTab,
    onTabSelected: (MainTab) -> Unit,
    onSettingsClick: () -> Unit,
    onAddClick: () -> Unit,
    modifier: Modifier = Modifier,
    content: @Composable (PaddingValues) -> Unit,
) {
    Scaffold(
        modifier = modifier,
        topBar = {
            ButlerCenteredTopAppBar(
                title = stringResource(currentTab.labelRes()),
                actions = {
                    IconButton(onClick = onSettingsClick) {
                        Icon(
                            imageVector = Icons.Default.Settings,
                            contentDescription = "Settings",
                        )
                    }
                },
            )
        },
        floatingActionButton = {
            FloatingActionButton(
                onClick = onAddClick,
            ) {
                Icon(Icons.Default.Add, contentDescription = "Add")
            }
        },
        bottomBar = {
            NavigationBar {
                MainTab.entries.forEach { tab ->
                    NavigationBarItem(
                        selected = currentTab == tab,
                        onClick = { onTabSelected(tab) },
                        icon = { Icon(tab.icon(), contentDescription = stringResource(tab.labelRes())) },
                        label = { Text(stringResource(tab.labelRes())) },
                        modifier = Modifier.testTag("BottomNav_${tab.name}"),
                    )
                }
            }
        },
        content = content,
    )
}

@Composable
fun DevicesScreen(
    state: HomeUiState,
    onTabSelected: (MainTab) -> Unit,
    onSettingsClick: () -> Unit,
    onAddDeviceClick: () -> Unit,
    onDeviceClick: (String) -> Unit,
    onGroupOptionToggle: () -> Unit,
    onGroupOptionSelected: (GroupOption) -> Unit,
    onSortOptionToggle: () -> Unit,
    onSortOptionSelected: (SortOption) -> Unit,
) {
    MainScreenShell(
        currentTab = MainTab.Devices,
        onTabSelected = onTabSelected,
        onSettingsClick = onSettingsClick,
        onAddClick = onAddDeviceClick,
    ) { innerPadding ->
        HomeScreenContent(
            state = state,
            onGroupOptionToggle = onGroupOptionToggle,
            onGroupOptionSelected = onGroupOptionSelected,
            onSortOptionToggle = onSortOptionToggle,
            onSortOptionSelected = onSortOptionSelected,
            onDeviceClick = onDeviceClick,
            modifier = Modifier.padding(innerPadding),
        )
    }
}

@Composable
fun TypesScreen(
    state: DeviceTypeListUiState,
    onTabSelected: (MainTab) -> Unit,
    onSettingsClick: () -> Unit,
    onAddTypeClick: () -> Unit,
    onEditType: (String) -> Unit,
    onSortOptionSelected: (DeviceTypeSortOption) -> Unit,
    onGroupOptionSelected: (DeviceTypeGroupOption) -> Unit,
    onSortDirectionToggle: () -> Unit,
    onGroupDirectionToggle: () -> Unit,
) {
    MainScreenShell(
        currentTab = MainTab.Types,
        onTabSelected = onTabSelected,
        onSettingsClick = onSettingsClick,
        onAddClick = onAddTypeClick,
    ) { innerPadding ->
        DeviceTypeListContent(
            state = state,
            onEditType = onEditType,
            onSortOptionSelected = onSortOptionSelected,
            onGroupOptionSelected = onGroupOptionSelected,
            onSortDirectionToggle = onSortDirectionToggle,
            onGroupDirectionToggle = onGroupDirectionToggle,
            modifier = Modifier.padding(innerPadding),
        )
    }
}

@Composable
fun HistoryScreen(
    state: HistoryListUiState,
    onTabSelected: (MainTab) -> Unit,
    onSettingsClick: () -> Unit,
    onAddEventClick: () -> Unit,
    onEventClick: (String, String) -> Unit,
) {
    MainScreenShell(
        currentTab = MainTab.History,
        onTabSelected = onTabSelected,
        onSettingsClick = onSettingsClick,
        onAddClick = onAddEventClick,
    ) { innerPadding ->
        HistoryListContent(
            state = state,
            onEventClick = onEventClick,
            modifier = Modifier.padding(innerPadding),
        )
    }
}

@Preview
@Preview
@Composable
fun DevicesScreenPreview() {
    BatteryButlerTheme {
        // Wrap the preview in the theme
        val now = Instant.parse("2026-01-18T17:00:00Z")
        val type = DeviceType("type1", "Smoke Alarm", "detector_smoke")
        val device = Device("dev1", "Kitchen Smoke", "type1", now, now, "Kitchen")
        val state = HomeUiState(
            groupedDevices = mapOf("All" to listOf(device)),
            deviceTypes = mapOf("type1" to type),
        )
        DevicesScreen(
            state = state,
            onTabSelected = {},
            onSettingsClick = {},
            onAddDeviceClick = {},
            onDeviceClick = {},
            onGroupOptionToggle = {},
            onGroupOptionSelected = {},
            onSortOptionToggle = {},
            onSortOptionSelected = {},
        )
    }
}

@Preview
@Composable
fun TypesScreenPreview() {
    BatteryButlerTheme {
        // Wrap the preview in the theme
        val type = DeviceType("type1", "Smoke Alarm", "detector_smoke")
        val state = DeviceTypeListUiState.Success(
            groupedTypes = mapOf("All" to listOf(type)),
        )
        TypesScreen(
            state = state,
            onTabSelected = {},
            onSettingsClick = {},
            onAddTypeClick = {},
            onEditType = {},
            onSortOptionSelected = {},
            onGroupOptionSelected = {},
            onSortDirectionToggle = {},
            onGroupDirectionToggle = {},
        )
    }
}

@OptIn(ExperimentalTime::class)
@Preview
@Composable
fun HistoryScreenPreview() {
    BatteryButlerTheme {
        // Wrap the preview in the theme
        val now = Instant.parse("2026-01-18T17:00:00Z")
        val event = BatteryEvent("evt1", "dev1", now)
        val item = HistoryItemUiModel(event, "Kitchen Smoke", "Smoke Alarm", "Kitchen")
        val state = HistoryListUiState.Success(
            items = listOf(item),
        )
        HistoryScreen(
            state = state,
            onTabSelected = {},
            onSettingsClick = {},
            onAddEventClick = {},
            onEventClick = { _, _ -> },
        )
    }
}
