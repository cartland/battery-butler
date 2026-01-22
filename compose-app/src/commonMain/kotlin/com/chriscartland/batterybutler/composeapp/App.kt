package com.chriscartland.batterybutler.composeapp

import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.runtime.Composable
import androidx.compose.runtime.CompositionLocalProvider
import androidx.lifecycle.viewmodel.compose.viewModel
import androidx.navigation.compose.NavHost
import androidx.navigation.compose.composable
import androidx.navigation.compose.rememberNavController
import androidx.navigation.toRoute
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
// Replaced by ScreenListSaver in NavigationSavers.kt

            val navController = rememberNavController()

            NavHost(
                navController = navController,
                startDestination = Screen.Devices,
            ) {
                // Shared navigation actions
                val navigateToDevices = {
                    navController.navigate(Screen.Devices) {
                        popUpTo(navController.graph.startDestinationId) {
                            inclusive = true
                            saveState = false // Reset state for main tabs
                        }
                        launchSingleTop = true
                    }
                }
                val navigateToTypes = {
                    navController.navigate(Screen.Types) {
                        popUpTo(navController.graph.startDestinationId) {
                            saveState = true
                        }
                        launchSingleTop = true
                        restoreState = true
                    }
                }
                val navigateToHistory = {
                    navController.navigate(Screen.History) {
                        popUpTo(navController.graph.startDestinationId) {
                            saveState = true
                        }
                        launchSingleTop = true
                        restoreState = true
                    }
                }

                val onTabSelected: (MainTab) -> Unit = { selectedTab ->
                    when (selectedTab) {
                        MainTab.Devices -> navigateToDevices()
                        MainTab.Types -> navigateToTypes()
                        MainTab.History -> navigateToHistory()
                    }
                }

                composable<Screen.Devices> {
                    val homeViewModel = viewModel { component.homeViewModel }
                    DevicesScreenRoot(
                        viewModel = homeViewModel,
                        onTabSelected = onTabSelected,
                        onSettingsClick = { navController.navigate(Screen.Settings) },
                        onAddDeviceClick = { navController.navigate(Screen.AddDevice) },
                        onDeviceClick = { deviceId ->
                            navController.navigate(Screen.DeviceDetail(deviceId))
                        },
                    )
                }

                composable<Screen.Types> {
                    val deviceTypeListViewModel = viewModel { component.deviceTypeListViewModel }
                    TypesScreenRoot(
                        viewModel = deviceTypeListViewModel,
                        onTabSelected = onTabSelected,
                        onSettingsClick = { navController.navigate(Screen.Settings) },
                        onAddTypeClick = { navController.navigate(Screen.AddDeviceType) },
                        onEditType = { typeId -> navController.navigate(Screen.EditDeviceType(typeId)) },
                    )
                }

                composable<Screen.History> {
                    val historyListViewModel = viewModel { component.historyListViewModel }
                    HistoryScreenRoot(
                        viewModel = historyListViewModel,
                        onTabSelected = onTabSelected,
                        onSettingsClick = { navController.navigate(Screen.Settings) },
                        onAddEventClick = { navController.navigate(Screen.AddBatteryEvent) },
                        onEventClick = { eventId, deviceId ->
                            navController.navigate(Screen.EventDetail(eventId))
                        },
                    )
                }

                composable<Screen.AddDevice> {
                    AddDeviceScreen(
                        viewModel = viewModel { component.addDeviceViewModel },
                        onDeviceAdded = { navController.popBackStack() },
                        onManageDeviceTypesClick = { navController.navigate(Screen.Types) },
                        onBack = { navController.popBackStack() },
                    )
                }

                composable<Screen.AddBatteryEvent> {
                    AddBatteryEventScreen(
                        viewModel = viewModel { component.addBatteryEventViewModel },
                        onEventAdded = { navController.popBackStack() },
                        onAddDeviceClick = { navController.navigate(Screen.AddDevice) },
                        onBack = { navController.popBackStack() },
                    )
                }

                composable<Screen.AddDeviceType> {
                    AddDeviceTypeScreen(
                        viewModel = viewModel { component.addDeviceTypeViewModel },
                        onDeviceTypeAdded = { navController.popBackStack() },
                        onBack = { navController.popBackStack() },
                    )
                }

                composable<Screen.DeviceDetail> { backStackEntry ->
                    val args = backStackEntry.toRoute<Screen.DeviceDetail>()
                    val viewModel = viewModel(key = "DeviceDetail-${args.deviceId}") {
                        component.deviceDetailViewModelFactory.create(args.deviceId)
                    }
                    DeviceDetailScreen(
                        viewModel = viewModel,
                        onBack = { navController.popBackStack() },
                        onEdit = { navController.navigate(Screen.EditDevice(args.deviceId)) },
                        onEventClick = { eventId -> navController.navigate(Screen.EventDetail(eventId)) },
                    )
                }

                composable<Screen.EventDetail> { backStackEntry ->
                    val args = backStackEntry.toRoute<Screen.EventDetail>()
                    val viewModel = viewModel(key = "EventDetail-${args.eventId}") {
                        component.eventDetailViewModelFactory.create(args.eventId)
                    }
                    EventDetailScreen(
                        viewModel = viewModel,
                        onBack = { navController.popBackStack() },
                    )
                }

                composable<Screen.EditDevice> { backStackEntry ->
                    val args = backStackEntry.toRoute<Screen.EditDevice>()
                    val viewModel = viewModel(key = "EditDevice-${args.deviceId}") {
                        component.editDeviceViewModelFactory.create(args.deviceId)
                    }
                    EditDeviceScreen(
                        viewModel = viewModel,
                        onBack = { navController.popBackStack() },
                        onDelete = {
                            navController.popBackStack()
                            // If we came from detail, pop again? Or handle navigation stack structure properly?
                            // Nav2 handles backstack automatically, but if we want to ensure we don't go back to deleted device...
                            // Actually, standard behavior is fine for now. If user navigates back to list, it's fine.
                            // If stack was List -> Detail -> Edit, jumping back to List is:
                            navController.popBackStack(Screen.Devices, inclusive = false)
                        },
                        onManageDeviceTypesClick = { navController.navigate(Screen.Types) },
                    )
                }

                composable<Screen.EditDeviceType> { backStackEntry ->
                    val args = backStackEntry.toRoute<Screen.EditDeviceType>()
                    val viewModel = viewModel(key = "EditDeviceType-${args.typeId}") {
                        component.editDeviceTypeViewModelFactory.create(args.typeId)
                    }
                    EditDeviceTypeScreen(
                        viewModel = viewModel,
                        onBack = { navController.popBackStack() },
                        onDelete = { navController.popBackStack() },
                    )
                }

                composable<Screen.Settings> {
                    SettingsScreen(
                        viewModel = viewModel { component.settingsViewModel },
                        onBack = { navController.popBackStack() },
                    )
                }
            }
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
