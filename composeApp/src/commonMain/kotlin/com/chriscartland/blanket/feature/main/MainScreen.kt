package com.chriscartland.blanket.feature.main

import androidx.compose.foundation.layout.padding
import androidx.compose.material.icons.Icons
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
import androidx.compose.material3.TopAppBar
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import com.chriscartland.blanket.feature.history.HistoryListScreen
import com.chriscartland.blanket.feature.history.HistoryListViewModel
import com.chriscartland.blanket.feature.home.HomeScreen
import com.chriscartland.blanket.feature.home.HomeViewModel

enum class MainTab(val label: String, val icon: androidx.compose.ui.graphics.vector.ImageVector) {
    Devices("Devices", Icons.Default.Home),
    History("History", Icons.Default.History)
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun MainScreen(
    homeViewModel: HomeViewModel,
    historyListViewModel: HistoryListViewModel,
    onAddDeviceClick: () -> Unit,
    onDeviceClick: (String) -> Unit,
    onManageTypesClick: () -> Unit,
    onEventClick: (String, String) -> Unit,
    modifier: Modifier = Modifier,
) {
    var currentTab by remember { mutableStateOf(MainTab.Devices) }

    Scaffold(
        modifier = modifier,
        topBar = {
            TopAppBar(
                title = { Text(currentTab.label) },
                actions = {
                    if (currentTab == MainTab.Devices) {
                        IconButton(onClick = onManageTypesClick) {
                            Icon(Icons.Default.Settings, contentDescription = "Manage Types")
                        }
                    }
                }
            )
        },
        floatingActionButton = {
            if (currentTab == MainTab.Devices) {
                FloatingActionButton(onClick = onAddDeviceClick) {
                    Icon(Icons.Default.Add, contentDescription = "Add Device")
                }
            }
        },
        bottomBar = {
            NavigationBar {
                MainTab.entries.forEach { tab ->
                    NavigationBarItem(
                        selected = currentTab == tab,
                        onClick = { currentTab = tab },
                        icon = { Icon(tab.icon, contentDescription = tab.label) },
                        label = { Text(tab.label) }
                    )
                }
            }
        }
    ) { innerPadding ->
        when (currentTab) {
            MainTab.Devices -> {
                HomeScreen(
                    viewModel = homeViewModel,
                    onAddDeviceClick = {}, // Handled by FAB in MainScreen
                    onDeviceClick = onDeviceClick,
                    onManageTypesClick = {}, // Handled by TopBar in MainScreen
                    modifier = Modifier.padding(innerPadding)
                )
            }
            MainTab.History -> {
                HistoryListScreen(
                    viewModel = historyListViewModel,
                    onEventClick = onEventClick,
                    modifier = Modifier.padding(innerPadding)
                )
            }
        }
    }
}
