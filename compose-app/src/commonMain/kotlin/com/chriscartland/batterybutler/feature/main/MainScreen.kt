package com.chriscartland.batterybutler.feature.main

import androidx.compose.foundation.layout.padding
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Add
import androidx.compose.material.icons.filled.History
import androidx.compose.material.icons.filled.Home
import androidx.compose.material.icons.filled.List
import androidx.compose.material.icons.filled.Settings
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.FloatingActionButton
import androidx.compose.material3.Icon
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
import com.chriscartland.batterybutler.feature.history.HistoryListScreen
import com.chriscartland.batterybutler.feature.home.HomeScreen
import com.chriscartland.batterybutler.presentation.core.components.ButlerCenteredTopAppBar
import com.chriscartland.batterybutler.viewmodel.devicetypes.DeviceTypeListViewModel
import com.chriscartland.batterybutler.viewmodel.history.HistoryListViewModel
import com.chriscartland.batterybutler.viewmodel.home.HomeViewModel

@kotlinx.serialization.Serializable
enum class MainTab(
    val label: String,
    val icon: androidx.compose.ui.graphics.vector.ImageVector,
) {
    Devices("Devices", Icons.Default.Home),
    Types("Types", Icons.Default.List),
    History("History", Icons.Default.History),
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
                com.chriscartland.batterybutler.feature.devicetypes.DeviceTypeListScreen(
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
    content: @Composable (androidx.compose.foundation.layout.PaddingValues) -> Unit,
) {
    Scaffold(
        modifier = modifier,
        topBar = {
            ButlerCenteredTopAppBar(
                title = currentTab.label,
                actions = {
                    androidx.compose.material3.IconButton(onClick = onSettingsClick) {
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
                        icon = { Icon(tab.icon, contentDescription = tab.label) },
                        label = { Text(tab.label) },
                    )
                }
            }
        },
        content = content,
    )
}
