package com.chriscartland.blanket

import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import com.chriscartland.blanket.di.AppComponent
import com.chriscartland.blanket.feature.adddevice.AddDeviceScreen
import com.chriscartland.blanket.feature.main.MainTab
import com.chriscartland.blanket.ui.theme.BlanketTheme
import org.jetbrains.compose.ui.tooling.preview.Preview

@Composable
@Preview
fun App(component: AppComponent) {
    BlanketTheme {
        var currentScreen by remember { mutableStateOf<Screen>(Screen.Home()) }

        when (val screen = currentScreen) {
            is Screen.Home -> {
                val homeViewModel = remember { component.homeViewModel }
                val historyListViewModel = remember { component.historyListViewModel }
                val deviceTypeListViewModel = remember { component.deviceTypeListViewModel }
                com.chriscartland.blanket.feature.main.MainScreen(
                    homeViewModel = homeViewModel,
                    historyListViewModel = historyListViewModel,
                    deviceTypeListViewModel = deviceTypeListViewModel,
                    initialTab = screen.initialTab,
                    onAddDeviceClick = { currentScreen = Screen.AddDevice },
                    onDeviceClick = { deviceId -> currentScreen = Screen.DeviceDetail(deviceId) },
                    onAddTypeClick = { currentScreen = Screen.AddDeviceType(returnScreen = Screen.Home(MainTab.Types)) },
                    onEditTypeClick = { typeId -> currentScreen = Screen.EditDeviceType(typeId) },
                    onEventClick = { eventId, deviceId -> currentScreen = Screen.EventDetail(eventId, deviceId) },
                )
            }
            Screen.AddDevice -> {
                AddDeviceScreen(
                    viewModel = component.addDeviceViewModel,
                    onDeviceAdded = { currentScreen = Screen.Home() },
                    onAddDeviceTypeClick = { currentScreen = Screen.AddDeviceType(returnScreen = Screen.AddDevice) },
                    onBack = { currentScreen = Screen.Home() },
                )
            }
            is Screen.AddDeviceType -> {
                val returnScreen = (currentScreen as Screen.AddDeviceType).returnScreen
                com.chriscartland.blanket.feature.adddevicetype.AddDeviceTypeScreen(
                    viewModel = component.addDeviceTypeViewModel,
                    onDeviceTypeAdded = { currentScreen = returnScreen },
                    onBack = { currentScreen = returnScreen },
                )
            }
            is Screen.DeviceDetail -> {
                val detailScreen = currentScreen as Screen.DeviceDetail
                val viewModel = remember(detailScreen) {
                    component.deviceDetailViewModelFactory.create(detailScreen.deviceId)
                }
                com.chriscartland.blanket.feature.devicedetail.DeviceDetailScreen(
                    viewModel = viewModel,
                    onBack = { currentScreen = Screen.Home() },
                    onEdit = { currentScreen = Screen.EditDevice(detailScreen.deviceId) },
                    onEventClick = { eventId -> currentScreen = Screen.EventDetail(eventId, detailScreen.deviceId) },
                )
            }
            is Screen.EventDetail -> {
                val eventDetailScreen = currentScreen as Screen.EventDetail
                val viewModel = remember(eventDetailScreen) {
                    component.eventDetailViewModelFactory.create(eventDetailScreen.eventId)
                }
                com.chriscartland.blanket.feature.eventdetail.EventDetailScreen(
                    viewModel = viewModel,
                    onBack = { currentScreen = Screen.DeviceDetail(eventDetailScreen.deviceId) },
                )
            }
            is Screen.EditDevice -> {
                val editDeviceScreen = currentScreen as Screen.EditDevice
                val viewModel = remember(editDeviceScreen) {
                    component.editDeviceViewModelFactory.create(editDeviceScreen.deviceId)
                }
                com.chriscartland.blanket.feature.editdevice.EditDeviceScreen(
                    viewModel = viewModel,
                    onBack = { currentScreen = Screen.DeviceDetail(editDeviceScreen.deviceId) },
                    onDelete = { currentScreen = Screen.Home() },
                )
            }
            // Screen.DeviceTypeList removed as it is now part of Home
            is Screen.EditDeviceType -> {
                val editTypeScreen = currentScreen as Screen.EditDeviceType
                val viewModel = remember(editTypeScreen) {
                    component.editDeviceTypeViewModelFactory.create(editTypeScreen.typeId)
                }
                com.chriscartland.blanket.feature.devicetypes.EditDeviceTypeScreen(
                    viewModel = viewModel,
                    onBack = { currentScreen = Screen.Home(MainTab.Types) },
                    onDelete = { currentScreen = Screen.Home(MainTab.Types) },
                )
            }
        }
    }
}

sealed interface Screen {
    data class Home(
        val initialTab: MainTab = MainTab.Devices,
    ) : Screen

    data object AddDevice : Screen

    data class AddDeviceType(
        val returnScreen: Screen,
    ) : Screen

    data class DeviceDetail(
        val deviceId: String,
    ) : Screen

    data class EditDevice(
        val deviceId: String,
    ) : Screen

    data class EventDetail(
        val eventId: String,
        val deviceId: String,
    ) : Screen

    data class EditDeviceType(
        val typeId: String,
    ) : Screen
}
