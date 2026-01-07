package com.chriscartland.batterybutler

import androidx.compose.runtime.Composable
import androidx.compose.runtime.CompositionLocalProvider
import androidx.compose.runtime.mutableStateListOf
import androidx.compose.runtime.remember
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import androidx.navigation3.runtime.entryProvider
import androidx.navigation3.ui.NavDisplay
import com.chriscartland.batterybutler.di.AppComponent
import com.chriscartland.batterybutler.feature.adddevice.AddDeviceScreen
import com.chriscartland.batterybutler.feature.ai.AiScreen
import com.chriscartland.batterybutler.feature.main.MainTab
import com.chriscartland.batterybutler.ui.theme.BatteryButlerTheme
import com.chriscartland.batterybutler.ui.theme.LocalAiAction
import com.chriscartland.batterybutler.ui.theme.LocalAiAvailable
import kotlinx.serialization.Serializable
import org.jetbrains.compose.ui.tooling.preview.Preview

@Composable
@Preview
fun App(component: AppComponent) {
    BatteryButlerTheme {
        val backStack = remember { mutableStateListOf<Any>(Screen.Home()) }
        val aiViewModel = remember { component.aiViewModel }
        val isAiAvailable = aiViewModel.isAiAvailable.collectAsStateWithLifecycle().value

        CompositionLocalProvider(
            LocalAiAvailable provides isAiAvailable,
            LocalAiAction provides { backStack.add(Screen.AiChat) },
        ) {
            NavDisplay(
                backStack = backStack,
                onBack = { backStack.removeLastOrNull() },
                entryProvider = entryProvider {
                    entry<Screen.Home> {
                        val homeArgs = it
                        val homeViewModel = remember { component.homeViewModel }
                        val historyListViewModel = remember { component.historyListViewModel }
                        // deviceTypeListViewModel no longer needed for MainScreen

                        com.chriscartland.batterybutler.feature.main.MainScreen(
                            homeViewModel = homeViewModel,
                            historyListViewModel = historyListViewModel,
                            initialTab = homeArgs.initialTab,
                            onAddDeviceClick = { backStack.add(Screen.AddDevice) },
                            onDeviceClick = { deviceId -> backStack.add(Screen.DeviceDetail(deviceId)) },
                            onEventClick = { eventId, deviceId -> backStack.add(Screen.EventDetail(eventId, deviceId)) },
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

                    entry<Screen.DeviceTypeList> {
                        val viewModel = remember { component.deviceTypeListViewModel }
                        com.chriscartland.batterybutler.feature.devicetypes.DeviceTypeListScreen(
                            viewModel = viewModel,
                            onEditType = { typeId -> backStack.add(Screen.EditDeviceType(typeId)) },
                            onAddType = {
                                backStack.add(Screen.AddDeviceType(returnScreen = Screen.DeviceTypeList))
                            },
                            onBack = { backStack.removeLastOrNull() },
                        )
                    }

                    entry<Screen.AddDeviceType> {
                        val args = it
                        // returnScreen arg is technically available but backStack logic handles the flow naturally now

                        com.chriscartland.batterybutler.feature.adddevicetype.AddDeviceTypeScreen(
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
                        com.chriscartland.batterybutler.feature.devicedetail.DeviceDetailScreen(
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
                        com.chriscartland.batterybutler.feature.eventdetail.EventDetailScreen(
                            viewModel = viewModel,
                            onBack = { backStack.removeLastOrNull() },
                        )
                    }

                    entry<Screen.EditDevice> {
                        val args = it
                        val viewModel = remember(args.deviceId) {
                            component.editDeviceViewModelFactory.create(args.deviceId)
                        }
                        com.chriscartland.batterybutler.feature.editdevice.EditDeviceScreen(
                            viewModel = viewModel,
                            onBack = { backStack.removeLastOrNull() },
                            onDelete = {
                                // Pop back to Home (removing DeviceDetail)
                                // Remove EditDevice
                                backStack.removeLastOrNull()
                                // Remove DeviceDetail if present below
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
                        com.chriscartland.batterybutler.feature.devicetypes.EditDeviceTypeScreen(
                            viewModel = viewModel,
                            onBack = { backStack.removeLastOrNull() },
                            onDelete = { backStack.removeLastOrNull() },
                        )
                    }

                    entry<Screen.AiChat> {
                        AiScreen(
                            viewModel = aiViewModel,
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
    data object AddDevice : Screen

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

    @Serializable
    data object AiChat : Screen
}
