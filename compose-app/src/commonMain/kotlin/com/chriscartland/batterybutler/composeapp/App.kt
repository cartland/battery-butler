package com.chriscartland.batterybutler.composeapp

import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.animation.togetherWith
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.runtime.Composable
import androidx.compose.runtime.CompositionLocalProvider
import androidx.compose.runtime.mutableStateListOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.saveable.listSaver
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.snapshots.SnapshotStateList
import androidx.compose.runtime.toMutableStateList
import androidx.lifecycle.viewmodel.compose.viewModel
import androidx.lifecycle.viewmodel.navigation3.rememberViewModelStoreNavEntryDecorator
import androidx.navigation3.runtime.entryProvider
import androidx.navigation3.runtime.rememberSaveableStateHolderNavEntryDecorator
import androidx.navigation3.ui.NavDisplay
import com.chriscartland.batterybutler.composeapp.di.AppComponent
import com.chriscartland.batterybutler.composeapp.feature.addbatteryevent.AddBatteryEventScreen
import com.chriscartland.batterybutler.composeapp.feature.adddevice.AddDeviceScreen
import com.chriscartland.batterybutler.composeapp.feature.adddevicetype.AddDeviceTypeScreen
import com.chriscartland.batterybutler.composeapp.feature.devicedetail.DeviceDetailScreen
import com.chriscartland.batterybutler.composeapp.feature.devicetypes.EditDeviceTypeScreen
import com.chriscartland.batterybutler.composeapp.feature.editdevice.EditDeviceScreen
import com.chriscartland.batterybutler.composeapp.feature.eventdetail.EventDetailScreen
import com.chriscartland.batterybutler.composeapp.feature.main.DevicesScreenRoot
import com.chriscartland.batterybutler.composeapp.feature.main.HistoryScreenRoot
import com.chriscartland.batterybutler.composeapp.feature.main.TypesScreenRoot
import com.chriscartland.batterybutler.composeapp.feature.settings.SettingsScreen
import com.chriscartland.batterybutler.composeresources.LocalAppStrings
import com.chriscartland.batterybutler.presentationcore.theme.BatteryButlerTheme
import com.chriscartland.batterybutler.presentationcore.util.FileSaver
import com.chriscartland.batterybutler.presentationcore.util.LocalFileSaver
import com.chriscartland.batterybutler.presentationcore.util.LocalShareHandler
import com.chriscartland.batterybutler.presentationcore.util.ShareHandler
import com.chriscartland.batterybutler.presentationfeature.main.MainTab
import kotlinx.serialization.Serializable
import kotlinx.serialization.encodeToString
import kotlinx.serialization.json.Json

// Preview removed as we can't easily preview with DI and Interfaces
@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun App(
    component: AppComponent,
    shareHandler: ShareHandler,
    fileSaver: FileSaver,
) {
    BatteryButlerTheme {
        CompositionLocalProvider(
            LocalShareHandler provides shareHandler,
            LocalFileSaver provides fileSaver,
            LocalAppStrings provides ComposeAppStrings(),
        ) {
            val backStackSaver = listSaver<SnapshotStateList<Any>, String>(
                save = { stateList ->
                    val screens = stateList.filterIsInstance<Screen>()
                    listOf(Json.encodeToString(screens))
                },
                restore = { restoredList ->
                    try {
                        val jsonString = restoredList.first()
                        val list: List<Screen> = Json.decodeFromString(jsonString)
                        val snapshotList = mutableStateListOf<Any>()
                        snapshotList.addAll(list)
                        snapshotList
                    } catch (e: Exception) {
                        mutableStateListOf(Screen.Devices)
                    }
                },
            )

            val backStack = rememberSaveable(saver = backStackSaver) {
                mutableStateListOf<Any>(Screen.Devices)
            }

            NavDisplay(
                backStack = backStack,
                onBack = { backStack.removeLastOrNull() },
                entryDecorators = listOf(
                    rememberSaveableStateHolderNavEntryDecorator(),
                    rememberViewModelStoreNavEntryDecorator(),
                ),
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

                    val onTabSelected: (MainTab) -> Unit = { selectedTab ->
                        when (selectedTab) {
                            MainTab.Devices -> navigateToDevices()
                            MainTab.Types -> navigateToTypes()
                            MainTab.History -> navigateToHistory()
                        }
                    }

                    entry<Screen.Devices> {
                        val homeViewModel = viewModel { component.homeViewModel }
                        DevicesScreenRoot(
                            viewModel = homeViewModel,
                            onTabSelected = onTabSelected,
                            onSettingsClick = { backStack.add(Screen.Settings) },
                            onAddDeviceClick = { backStack.add(Screen.AddDevice) },
                            onDeviceClick = { deviceId ->
                                backStack.add(Screen.DeviceDetail(deviceId))
                            },
                        )
                    }

                    entry<Screen.Types> {
                        val deviceTypeListViewModel = viewModel { component.deviceTypeListViewModel }
                        TypesScreenRoot(
                            viewModel = deviceTypeListViewModel,
                            onTabSelected = onTabSelected,
                            onSettingsClick = { backStack.add(Screen.Settings) },
                            onAddTypeClick = { backStack.add(Screen.AddDeviceType) },
                            onEditType = { typeId -> backStack.add(Screen.EditDeviceType(typeId)) },
                        )
                    }

                    entry<Screen.History> {
                        val historyListViewModel = viewModel { component.historyListViewModel }
                        HistoryScreenRoot(
                            viewModel = historyListViewModel,
                            onTabSelected = onTabSelected,
                            onSettingsClick = { backStack.add(Screen.Settings) },
                            onAddEventClick = { backStack.add(Screen.AddBatteryEvent) },
                            onEventClick = { eventId, deviceId ->
                                backStack.add(Screen.EventDetail(eventId))
                            },
                        )
                    }

                    entry<Screen.AddDevice> {
                        AddDeviceScreen(
                            viewModel = viewModel { component.addDeviceViewModel },
                            onDeviceAdded = { backStack.removeLastOrNull() },
                            onManageDeviceTypesClick = { backStack.add(Screen.Types) },
                            onBack = { backStack.removeLastOrNull() },
                        )
                    }

                    entry<Screen.AddBatteryEvent> {
                        AddBatteryEventScreen(
                            viewModel = viewModel { component.addBatteryEventViewModel },
                            onEventAdded = { backStack.removeLastOrNull() },
                            onAddDeviceClick = { backStack.add(Screen.AddDevice) },
                            onBack = { backStack.removeLastOrNull() },
                        )
                    }

                    entry<Screen.AddDeviceType> {
                        AddDeviceTypeScreen(
                            viewModel = viewModel { component.addDeviceTypeViewModel },
                            onDeviceTypeAdded = { backStack.removeLastOrNull() },
                            onBack = { backStack.removeLastOrNull() },
                        )
                    }

                    entry<Screen.DeviceDetail> {
                        val args = it
                        val viewModel = viewModel(key = "DeviceDetail-${args.deviceId}") {
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
                        val viewModel = viewModel(key = "EventDetail-${args.eventId}") {
                            component.eventDetailViewModelFactory.create(args.eventId)
                        }
                        EventDetailScreen(
                            viewModel = viewModel,
                            onBack = { backStack.removeLastOrNull() },
                        )
                    }

                    entry<Screen.EditDevice> {
                        val args = it
                        val viewModel = viewModel(key = "EditDevice-${args.deviceId}") {
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
                        val viewModel = viewModel(key = "EditDeviceType-${args.typeId}") {
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
                            viewModel = viewModel { component.settingsViewModel },
                            onBack = { backStack.removeLastOrNull() },
                        )
                    }
                },
                transitionSpec = {
                    fadeIn() togetherWith fadeOut()
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
