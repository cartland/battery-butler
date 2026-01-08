package com.chriscartland.batterybutler

import androidx.compose.foundation.layout.padding
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Add
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.FloatingActionButton
import androidx.compose.material3.Icon
import androidx.compose.material3.Scaffold
import androidx.compose.runtime.Composable
import androidx.compose.runtime.CompositionLocalProvider
import androidx.compose.runtime.mutableStateListOf
import androidx.compose.runtime.remember
import androidx.compose.ui.Modifier
import androidx.navigation3.runtime.entryProvider
import androidx.navigation3.ui.NavDisplay
import com.chriscartland.batterybutler.di.AppComponent
import com.chriscartland.batterybutler.feature.adddevice.AddDeviceScreen
import com.chriscartland.batterybutler.feature.main.MainTab
import com.chriscartland.batterybutler.ui.components.ButlerCenteredTopAppBar
import com.chriscartland.batterybutler.ui.theme.BatteryButlerTheme
import com.chriscartland.batterybutler.ui.util.LocalShareHandler
import com.chriscartland.batterybutler.ui.util.ShareHandler
import kotlinx.serialization.Serializable
import com.chriscartland.batterybutler.feature.main.MainScreen
import com.chriscartland.batterybutler.feature.addbatteryevent.AddBatteryEventScreen
import com.chriscartland.batterybutler.feature.devicetypes.DeviceTypeListScreen
import com.chriscartland.batterybutler.feature.adddevicetype.AddDeviceTypeScreen
import com.chriscartland.batterybutler.feature.devicedetail.DeviceDetailScreen
import com.chriscartland.batterybutler.feature.eventdetail.EventDetailScreen
import com.chriscartland.batterybutler.feature.editdevice.EditDeviceScreen
import com.chriscartland.batterybutler.feature.devicetypes.EditDeviceTypeScreen
import com.chriscartland.batterybutler.feature.settings.SettingsScreen

// Preview removed as we can't easily preview with DI and Interfaces
@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun App(
    component: AppComponent,
    shareHandler: ShareHandler,
) {
    BatteryButlerTheme {
        CompositionLocalProvider(LocalShareHandler provides shareHandler) {
            val backStack = remember { mutableStateListOf<Any>(Screen.Home()) }

            NavDisplay(
                backStack = backStack,
                onBack = { backStack.removeLastOrNull() },
                entryProvider = entryProvider {
                    entry<Screen.Home> {
                        val homeArgs = it
                        val homeViewModel = remember { component.homeViewModel }
                        val historyListViewModel = remember { component.historyListViewModel }
                        val deviceTypeListViewModel = remember { component.deviceTypeListViewModel }

                        MainScreen(
                            homeViewModel = homeViewModel,
                            historyListViewModel = historyListViewModel,
                            deviceTypeListViewModel = deviceTypeListViewModel,
                            initialTab = homeArgs.initialTab,
                            onAddDeviceClick = { backStack.add(Screen.AddDevice) },
                            onDeviceClick = { deviceId -> backStack.add(Screen.DeviceDetail(deviceId)) },
                            onEventClick = { eventId, deviceId ->
                                backStack.add(Screen.EventDetail(eventId, deviceId))
                            },
                            // New Actions
                            onAddTypeClick = {
                                backStack.add(
                                    Screen.AddDeviceType(
                                        returnScreen = Screen.Home(initialTab = MainTab.Types),
                                    ),
                                )
                            },
                            onEditTypeClick = { typeId -> backStack.add(Screen.EditDeviceType(typeId)) },
                            onAddEventClick = { backStack.add(Screen.AddBatteryEvent) },
                            onManageTypesClick = { backStack.add(Screen.DeviceTypeList) }, // Fallback if Types tab exists?
                            onSettingsClick = { backStack.add(Screen.Settings) },
                        )
                    }

                    entry<Screen.AddDevice> {
                        AddDeviceScreen(
                            viewModel = component.addDeviceViewModel,
                            onDeviceAdded = {
                                backStack.removeLastOrNull()
                            },
                            onManageDeviceTypesClick = { backStack.add(Screen.DeviceTypeList) },
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

                    entry<Screen.DeviceTypeList> {
                        val viewModel = remember { component.deviceTypeListViewModel }
                        Scaffold(
                            topBar = {
                                ButlerCenteredTopAppBar(
                                    title = "Device Types",
                                    onBack = { backStack.removeLastOrNull() },
                                )
                            },
                            floatingActionButton = {
                                FloatingActionButton(
                                    onClick = {
                                        backStack.add(Screen.AddDeviceType(returnScreen = Screen.DeviceTypeList))
                                    },
                                ) {
                                    Icon(Icons.Default.Add, contentDescription = "Add Type")
                                }
                            },
                        ) { innerPadding ->
                            DeviceTypeListScreen(
                                viewModel = viewModel,
                                onEditType = { typeId -> backStack.add(Screen.EditDeviceType(typeId)) },
                                modifier = Modifier.padding(innerPadding),
                            )
                        }
                    }

                    entry<Screen.AddDeviceType> {
                        val args = it
                        AddDeviceTypeScreen(
                            viewModel = component.addDeviceTypeViewModel,
                            onDeviceTypeAdded = {
                                backStack.removeLastOrNull()
                            },
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
                            onEventClick = { eventId -> backStack.add(Screen.EventDetail(eventId, args.deviceId)) },
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
    data class Home(
        val initialTab: MainTab = MainTab.Devices,
    ) : Screen

    @Serializable
    data object Settings : Screen

    @Serializable
    data object AddDevice : Screen

    @Serializable
    data object AddBatteryEvent : Screen

    @Serializable
    data class AddDeviceType(
        val returnScreen: Screen,
    ) : Screen

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
        val deviceId: String,
    ) : Screen

    @Serializable
    data class EditDeviceType(
        val typeId: String,
    ) : Screen

    @Serializable
    data object DeviceTypeList : Screen
}
