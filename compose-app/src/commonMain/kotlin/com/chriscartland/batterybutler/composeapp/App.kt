package com.chriscartland.batterybutler.composeapp

import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.animation.togetherWith
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.runtime.Composable
import androidx.compose.runtime.CompositionLocalProvider
import androidx.compose.runtime.mutableStateListOf
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.snapshots.SnapshotStateList
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
import com.chriscartland.batterybutler.composeapp.util.ScreenListSaver
import com.chriscartland.batterybutler.composeresources.LocalAppStrings
import com.chriscartland.batterybutler.presentationcore.theme.BatteryButlerTheme
import com.chriscartland.batterybutler.presentationcore.util.FileSaver
import com.chriscartland.batterybutler.presentationcore.util.LocalFileSaver
import com.chriscartland.batterybutler.presentationcore.util.LocalShareHandler
import com.chriscartland.batterybutler.presentationcore.util.ShareHandler
import com.chriscartland.batterybutler.presentationfeature.main.MainTab
import kotlinx.serialization.Serializable

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
            val backStack = rememberSaveable(saver = ScreenListSaver) {
                mutableStateListOf<Screen>(Screen.Devices)
            }

            NavDisplay(
                backStack = backStack,
                onBack = { backStack.removeLastOrNull() },
                entryDecorators = listOf(
                    rememberSaveableStateHolderNavEntryDecorator<Screen>(),
                    rememberViewModelStoreNavEntryDecorator<Screen>(),
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
                            onSettingsClick = { backStack.navigateTo(Screen.Settings) },
                            onAddDeviceClick = { backStack.navigateTo(Screen.AddDevice) },
                            onDeviceClick = { deviceId ->
                                backStack.navigateTo(Screen.DeviceDetail(deviceId))
                            },
                        )
                    }

                    entry<Screen.Types> {
                        val deviceTypeListViewModel = viewModel { component.deviceTypeListViewModel }
                        TypesScreenRoot(
                            viewModel = deviceTypeListViewModel,
                            onTabSelected = onTabSelected,
                            onSettingsClick = { backStack.navigateTo(Screen.Settings) },
                            onAddTypeClick = { backStack.navigateTo(Screen.AddDeviceType) },
                            onEditType = { typeId -> backStack.navigateTo(Screen.EditDeviceType(typeId)) },
                        )
                    }

                    entry<Screen.History> {
                        val historyListViewModel = viewModel { component.historyListViewModel }
                        HistoryScreenRoot(
                            viewModel = historyListViewModel,
                            onTabSelected = onTabSelected,
                            onSettingsClick = { backStack.navigateTo(Screen.Settings) },
                            onAddEventClick = { backStack.navigateTo(Screen.AddBatteryEvent) },
                            onEventClick = { eventId, deviceId ->
                                backStack.navigateTo(Screen.EventDetail(eventId))
                            },
                        )
                    }

                    entry<Screen.AddDevice> {
                        AddDeviceScreen(
                            viewModel = viewModel { component.addDeviceViewModel },
                            onDeviceAdded = { backStack.removeLastOrNull() },
                            onManageDeviceTypesClick = { backStack.navigateTo(Screen.Types) },
                            onBack = { backStack.removeLastOrNull() },
                        )
                    }

                    entry<Screen.AddBatteryEvent> {
                        AddBatteryEventScreen(
                            viewModel = viewModel { component.addBatteryEventViewModel },
                            onEventAdded = { backStack.removeLastOrNull() },
                            onAddDeviceClick = { backStack.navigateTo(Screen.AddDevice) },
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
                            onEdit = { backStack.navigateTo(Screen.EditDevice(args.deviceId)) },
                            onEventClick = { eventId -> backStack.navigateTo(Screen.EventDetail(eventId)) },
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
                            onManageDeviceTypesClick = { backStack.navigateTo(Screen.Types) },
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

/**
 * Safely navigates to a screen, preventing duplicate navigation from rapid clicks.
 * If the last screen in the back stack matches the target screen, the navigation is skipped.
 */
private fun SnapshotStateList<Screen>.navigateTo(screen: Screen) {
    if (lastOrNull() != screen) {
        add(screen)
    }
}
