package com.chriscartland.batterybutler.composeapp.feature.main

import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.padding
import batterybutler.compose_app.generated.resources.Res
import batterybutler.compose_app.generated.resources.tab_devices
import batterybutler.compose_app.generated.resources.tab_history
import batterybutler.compose_app.generated.resources.tab_types
import org.jetbrains.compose.resources.StringResource
import org.jetbrains.compose.resources.stringResource
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Add
import androidx.compose.material.icons.filled.History
import androidx.compose.material.icons.filled.Home
import androidx.compose.material.icons.automirrored.filled.List
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
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.graphics.vector.ImageVector
import com.chriscartland.batterybutler.composeapp.feature.devicetypes.DeviceTypeListScreen
import com.chriscartland.batterybutler.composeapp.feature.history.HistoryListScreen
import com.chriscartland.batterybutler.composeapp.feature.home.HomeScreen
import com.chriscartland.batterybutler.presentationcore.components.ButlerCenteredTopAppBar
import com.chriscartland.batterybutler.viewmodel.devicetypes.DeviceTypeListViewModel
import com.chriscartland.batterybutler.viewmodel.history.HistoryListViewModel
import com.chriscartland.batterybutler.viewmodel.home.HomeViewModel
import kotlinx.serialization.Serializable

@Serializable
enum class MainTab {
    Devices,
    Types,
    History,
}

fun MainTab.labelRes(): StringResource = when (this) {
    MainTab.Devices -> Res.string.tab_devices
    MainTab.Types -> Res.string.tab_types
    MainTab.History -> Res.string.tab_history
}

fun MainTab.icon(): ImageVector = when (this) {
    MainTab.Devices -> Icons.Default.Home
    MainTab.Types -> Icons.AutoMirrored.Filled.List
    MainTab.History -> Icons.Default.History
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun MainScreen(
    homeViewModel: HomeViewModel,
    historyListViewModel: HistoryListViewModel,
    deviceTypeListViewModel: DeviceTypeListViewModel,
    onAddDeviceClick: () -> Unit,
    onDeviceClick: (String) -> Unit,
    onEventClick: (String, String) -> Unit,
    onAddTypeClick: () -> Unit,
    onEditTypeClick: (String) -> Unit,
    onAddEventClick: () -> Unit,
    onSettingsClick: () -> Unit,
    onManageTypesClick: () -> Unit, // Potentially unused if handled by Types tab, but kept for compatibility
    initialTab: MainTab = MainTab.Devices,
    modifier: Modifier = Modifier,
) {
    var currentTab by remember { mutableStateOf(initialTab) }

    MainScreenShell(
        currentTab = currentTab,
        onTabSelected = { currentTab = it },
        onSettingsClick = onSettingsClick,
        onAddClick = {
            when (currentTab) {
                MainTab.Devices -> onAddDeviceClick()
                MainTab.Types -> onAddTypeClick()
                MainTab.History -> onAddEventClick()
            }
        },
    ) { innerPadding ->
        Modifier.padding(innerPadding)
        when (currentTab) {
            MainTab.Devices -> {
                HomeScreen(
                    viewModel = homeViewModel,
                    onAddDeviceClick = {}, // Handled by FAB in MainScreen
                    onDeviceClick = onDeviceClick,
                    onManageTypesClick = {}, // Removed from here
                    modifier = Modifier.padding(innerPadding),
                )
            }
            MainTab.Types -> {
                DeviceTypeListScreen(
                    viewModel = deviceTypeListViewModel,
                    onEditType = onEditTypeClick,
                    modifier = Modifier.padding(innerPadding),
                )
            }
            MainTab.History -> {
                HistoryListScreen(
                    viewModel = historyListViewModel,
                    onEventClick = onEventClick,
                    modifier = Modifier.padding(innerPadding),
                )
            }
        }
    }
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
                        // Tag uses raw enum name or we can use the resource key if we want, but name is safer for tests not running UI
                        modifier = Modifier.testTag("BottomNav_${tab.name}"), 
                    )
                }
            }
        },
        content = content,
    )
}
