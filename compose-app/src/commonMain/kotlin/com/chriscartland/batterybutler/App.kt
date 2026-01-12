package com.chriscartland.batterybutler

import androidx.compose.foundation.layout.padding
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.runtime.Composable
import androidx.compose.runtime.CompositionLocalProvider
import androidx.compose.runtime.mutableStateListOf
import androidx.compose.runtime.remember
import androidx.compose.ui.Modifier
import androidx.navigation3.runtime.entryProvider
import androidx.navigation3.ui.NavDisplay
import com.chriscartland.batterybutler.di.AppComponent
import com.chriscartland.batterybutler.feature.addbatteryevent.AddBatteryEventScreen
import com.chriscartland.batterybutler.feature.adddevice.AddDeviceScreen
import com.chriscartland.batterybutler.feature.adddevicetype.AddDeviceTypeScreen
import com.chriscartland.batterybutler.feature.devicedetail.DeviceDetailScreen
import com.chriscartland.batterybutler.feature.devicetypes.DeviceTypeListScreen
import com.chriscartland.batterybutler.feature.devicetypes.EditDeviceTypeScreen
import com.chriscartland.batterybutler.feature.editdevice.EditDeviceScreen
import com.chriscartland.batterybutler.feature.eventdetail.EventDetailScreen
import com.chriscartland.batterybutler.feature.main.MainTab
import com.chriscartland.batterybutler.feature.settings.SettingsScreen
import com.chriscartland.batterybutler.presentation.core.theme.BatteryButlerTheme
import com.chriscartland.batterybutler.presentation.core.util.LocalShareHandler
import com.chriscartland.batterybutler.presentation.core.util.ShareHandler
import kotlinx.serialization.Serializable

// Preview removed as we can't easily preview with DI and Interfaces
@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun App(
    component: AppComponent,
    shareHandler: ShareHandler,
) {
    BatteryButlerTheme {
        CompositionLocalProvider(LocalShareHandler provides shareHandler) {
            val backStack = remember { mutableStateListOf<Any>(Screen.Devices) }

            NavDisplay(
                backStack = backStack,
                onBack = { backStack.removeLastOrNull() },
                entryProvider = entryProvider {
                    // Shared navigation actions
                    val navigateToDevices = {
                        // Clear stack to root [Screen.Devices]
                        if (backStack.last() != Screen.Devices) {
                            backStack.clear()
                            backStack.add(Screen.Devices)
                        }
                    }
                    val navigateToTypes = {
                        // Stack: [Devices, Types]
                        backStack.clear()
                        backStack.add(Screen.Devices)
                        backStack.add(Screen.Types)
                    }
                    val navigateToHistory = {
                        // Stack: [Devices, History]
                        backStack.clear()
                        backStack.add(Screen.Devices)
                        backStack.add(Screen.History)
                    }

                    // Shell for main tabs
                    val showShell: @Composable (
                        MainTab,
                        @Composable (androidx.compose.foundation.layout.PaddingValues) -> Unit,
                    ) -> Unit = { tab, content ->
                        com.chriscartland.batterybutler.feature.main.MainScreenShell(
                            currentTab = tab,
                            onTabSelected = { selectedTab ->
                                when (selectedTab) {
                                    MainTab.Devices -> navigateToDevices()
                                    MainTab.Types -> navigateToTypes()
                                    MainTab.History -> navigateToHistory()
                                }
                            },
                            onSettingsClick = { backStack.add(Screen.Settings) },
                            onAddClick = {
                                when (tab) {
                                    MainTab.Devices -> backStack.add(Screen.AddDevice)
                                    MainTab.Types -> backStack.add(
                                        Screen.AddDeviceType,
                                    )
                                    MainTab.History -> backStack.add(Screen.AddBatteryEvent)
                                }
                            },
                            content = content,
                        )
                    }

                    entry<Screen.Devices> {
                        val homeViewModel = remember { component.homeViewModel }
                        showShell(MainTab.Devices) { innerPadding ->
                            com.chriscartland.batterybutler.feature.home.HomeScreen(
                                viewModel = homeViewModel,
                                onAddDeviceClick = {}, // Handled by FAB
                                onDeviceClick = { deviceId -> backStack.add(Screen.DeviceDetail(deviceId)) },
                                onManageTypesClick = {}, // Removed
                                modifier = Modifier.padding(innerPadding),
                            )
                        }
                    }

                    entry<Screen.Types> {
                        val deviceTypeListViewModel = remember { component.deviceTypeListViewModel }
                        showShell(MainTab.Types) { innerPadding ->
                            DeviceTypeListScreen(
                                viewModel = deviceTypeListViewModel,
                                onEditType = { typeId -> backStack.add(Screen.EditDeviceType(typeId)) },
                                modifier = Modifier.padding(innerPadding),
                            )
                        }
                    }

                    entry<Screen.History> {
                        val historyListViewModel = remember { component.historyListViewModel }
                        showShell(MainTab.History) { innerPadding ->
                            com.chriscartland.batterybutler.feature.history.HistoryListScreen(
                                viewModel = historyListViewModel,
                                onEventClick = { eventId, deviceId ->
                                    backStack.add(Screen.EventDetail(eventId))
                                },
                                modifier = Modifier.padding(innerPadding),
                            )
                        }
                    }

                    entry<Screen.AddDevice> {
                        AddDeviceScreen(
                            viewModel = component.addDeviceViewModel,
                            onDeviceAdded = { backStack.removeLastOrNull() },
                            onManageDeviceTypesClick = { backStack.add(Screen.Types) },
                            onBack = { backStack.removeLastOrNull() },
                        )
                    }

                    entry<Screen.AddBatteryEvent> {
                        AddBatteryEventScreen(
                            viewModel = component.addBatteryEventViewModel,
                            onEventAdded = { backStack.removeLastOrNull() },
                            onAddDeviceClick = { backStack.add(Screen.AddDevice) },
                            onBack = { backStack.removeLastOrNull() },
                        )
                    }

                    entry<Screen.AddDeviceType> {
                        AddDeviceTypeScreen(
                            viewModel = component.addDeviceTypeViewModel,
                            onDeviceTypeAdded = { backStack.removeLastOrNull() },
                            onBack = { backStack.removeLastOrNull() },
                        )
                    }

                    entry<Screen.DeviceDetail> {
                        val args = it
                        val viewModel = remember(args.deviceId) {
                            component.deviceDetailViewModelFactory.create(args.deviceId)
                        }
                        DeviceDetailScreen(
                            viewModel = viewModel,
                            onBack = { backStack.removeLastOrNull() },
                            onEdit = { backStack.add(Screen.EditDevice(args.deviceId)) },
                            onEventClick = { eventId -> backStack.add(Screen.EventDetail(eventId)) },
                        )
                    }

                    entry<Screen.EventDetail> {
                        val args = it
                        val viewModel = remember(args.eventId) {
                            component.eventDetailViewModelFactory.create(args.eventId)
                        }
                        EventDetailScreen(
                            viewModel = viewModel,
                            onBack = { backStack.removeLastOrNull() },
                        )
                    }

                    entry<Screen.EditDevice> {
                        val args = it
                        val viewModel = remember(args.deviceId) {
                            component.editDeviceViewModelFactory.create(args.deviceId)
                        }
                        EditDeviceScreen(
                            viewModel = viewModel,
                            onBack = { backStack.removeLastOrNull() },
                            onDelete = {
                                backStack.removeLastOrNull()
                                if (backStack.lastOrNull() is Screen.DeviceDetail) {
                                    backStack.removeLastOrNull()
                                }
                            },
                            onManageDeviceTypesClick = { backStack.add(Screen.Types) },
                        )
                    }

                    entry<Screen.EditDeviceType> {
                        val args = it
                        val viewModel = remember(args.typeId) {
                            component.editDeviceTypeViewModelFactory.create(args.typeId)
                        }
                        EditDeviceTypeScreen(
                            viewModel = viewModel,
                            onBack = { backStack.removeLastOrNull() },
                            onDelete = { backStack.removeLastOrNull() },
                        )
                    }

                    entry<Screen.Settings> {
                        SettingsScreen(
                            viewModel = component.settingsViewModel,
                            onBack = { backStack.removeLastOrNull() },
                        )
                    }
                },
            )
        }
    }
}

@Serializable
sealed interface Screen {
    @Serializable
    data object Devices : Screen

    @Serializable
    data object History : Screen

    @Serializable
    data object Types : Screen

    @Serializable
    data object Settings : Screen

    @Serializable
    data object AddDevice : Screen

    @Serializable
    data object AddBatteryEvent : Screen

    @Serializable
    data object AddDeviceType : Screen

    @Serializable
    data class DeviceDetail(
        val deviceId: String,
    ) : Screen

    @Serializable
    data class EditDevice(
        val deviceId: String,
    ) : Screen

    @Serializable
    data class EventDetail(
        val eventId: String,
    ) : Screen

    @Serializable
    data class EditDeviceType(
        val typeId: String,
    ) : Screen
}
