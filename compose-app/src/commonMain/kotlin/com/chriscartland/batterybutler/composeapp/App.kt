package com.chriscartland.batterybutler.composeapp

import androidx.compose.animation.core.tween
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.animation.slideInHorizontally
import androidx.compose.animation.slideOutHorizontally
import androidx.compose.animation.togetherWith
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.runtime.Composable
import androidx.compose.runtime.CompositionLocalProvider
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateListOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import androidx.lifecycle.viewmodel.compose.viewModel
import androidx.lifecycle.viewmodel.navigation3.rememberViewModelStoreNavEntryDecorator
import androidx.navigation3.runtime.entryProvider
import androidx.navigation3.runtime.rememberSaveableStateHolderNavEntryDecorator
import androidx.navigation3.ui.NavDisplay
import com.chriscartland.batterybutler.composeapp.components.BackHandler
import com.chriscartland.batterybutler.composeapp.di.AppComponent
import com.chriscartland.batterybutler.composeapp.feature.addbatteryevent.AddBatteryEventScreen
import com.chriscartland.batterybutler.composeapp.feature.adddevice.AddDeviceScreen
import com.chriscartland.batterybutler.composeapp.feature.adddevicetype.AddDeviceTypeScreen
import com.chriscartland.batterybutler.composeapp.feature.aichat.AiChatScreen
import com.chriscartland.batterybutler.composeapp.feature.devicedetail.DeviceDetailScreen
import com.chriscartland.batterybutler.composeapp.feature.devicetypes.DeviceTypeDetailScreen
import com.chriscartland.batterybutler.composeapp.feature.devicetypes.EditDeviceTypeScreen
import com.chriscartland.batterybutler.composeapp.feature.editdevice.EditDeviceScreen
import com.chriscartland.batterybutler.composeapp.feature.eventdetail.EditBatteryEventScreen
import com.chriscartland.batterybutler.composeapp.feature.eventdetail.EventDetailScreen
import com.chriscartland.batterybutler.composeapp.feature.login.LoginScreen
import com.chriscartland.batterybutler.composeapp.feature.main.DevicesScreenRoot
import com.chriscartland.batterybutler.composeapp.feature.main.HistoryScreenRoot
import com.chriscartland.batterybutler.composeapp.feature.main.TypesScreenRoot
import com.chriscartland.batterybutler.composeapp.feature.settings.SettingsScreen
import com.chriscartland.batterybutler.composeapp.navigation.Screen
import com.chriscartland.batterybutler.composeapp.navigation.navigateTo
import com.chriscartland.batterybutler.composeapp.util.ScreenListSaver
import com.chriscartland.batterybutler.composeresources.LocalAppStrings
import com.chriscartland.batterybutler.domain.model.FeatureFlag
import com.chriscartland.batterybutler.domain.model.ai.AiRole
import com.chriscartland.batterybutler.presentationcore.theme.BatteryButlerTheme
import com.chriscartland.batterybutler.presentationcore.theme.LocalAiAction
import com.chriscartland.batterybutler.presentationcore.theme.LocalAiAvailable
import com.chriscartland.batterybutler.presentationcore.util.FileSaver
import com.chriscartland.batterybutler.presentationcore.util.LocalFileSaver
import com.chriscartland.batterybutler.presentationcore.util.LocalShareHandler
import com.chriscartland.batterybutler.presentationcore.util.ShareHandler
import com.chriscartland.batterybutler.presentationfeature.aichat.ChatUiMessage
import com.chriscartland.batterybutler.presentationfeature.main.MainScreenShell
import com.chriscartland.batterybutler.presentationfeature.main.MainTab
import com.chriscartland.batterybutler.viewmodel.aichat.AiChatViewModel

private val slideTransitionMetadata =
    NavDisplay.transitionSpec {
        slideInHorizontally(tween(300)) { it } + fadeIn(tween(300)) togetherWith
            slideOutHorizontally(tween(300)) { -it / 3 } + fadeOut(tween(300))
    } + NavDisplay.popTransitionSpec {
        slideInHorizontally(tween(300)) { -it / 3 } + fadeIn(tween(300)) togetherWith
            slideOutHorizontally(tween(300)) { it } + fadeOut(tween(300))
    }

private fun Screen?.toMainTab(): MainTab =
    when (this) {
        is Screen.Types -> MainTab.Types
        is Screen.History -> MainTab.History
        else -> MainTab.Devices
    }

// Preview removed as we can't easily preview with DI and Interfaces
@OptIn(ExperimentalMaterial3Api::class)
@Suppress("ViewModelInjection")
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
            // Tab back stack: only Devices, Types, History
            val tabBackStack = rememberSaveable(saver = ScreenListSaver) {
                mutableStateListOf<Screen>(Screen.Devices)
            }
            // Detail back stack: Login and all full-screen detail screens
            val detailBackStack = rememberSaveable(saver = ScreenListSaver) {
                mutableStateListOf<Screen>(Screen.Login)
            }

            val isAiEnabled = component.featureFlagProvider.isEnabled(FeatureFlag.AI_BATCH_IMPORT)

            // Hoist AI ViewModel at App scope so it persists across tab switches
            val aiViewModel: AiChatViewModel = viewModel { component.aiChatViewModel }
            val aiMessages by aiViewModel.messages.collectAsStateWithLifecycle()
            val isAiProcessing by aiViewModel.isProcessing.collectAsStateWithLifecycle()
            var isAiExpanded by rememberSaveable { mutableStateOf(false) }
            var tabTransitionForward by rememberSaveable { mutableStateOf(true) }

            val aiUiMessages = aiMessages.map { msg ->
                ChatUiMessage(
                    id = msg.id,
                    text = msg.text,
                    isUser = msg.role == AiRole.USER,
                )
            }

            // Determine active tab name from current tab back stack
            val currentTabName = when (tabBackStack.lastOrNull()) {
                is Screen.Devices -> MainTab.Devices.name
                is Screen.Types -> MainTab.Types.name
                is Screen.History -> MainTab.History.name
                else -> null
            }

            val onSendAiMessage: (String) -> Unit = { text ->
                val hints = buildMap {
                    currentTabName?.let { put("Message sent from", "$it tab") }
                }
                aiViewModel.sendMessage(text, hints = hints)
            }

            // Tab navigation actions
            val navigateToDevices = {
                tabTransitionForward = false // always going to index 0 (leftward)
                if (tabBackStack.lastOrNull() != Screen.Devices) {
                    tabBackStack.clear()
                    tabBackStack.add(Screen.Devices)
                }
            }
            val navigateToTypes = {
                // Going forward unless currently on History (index 2 > 1)
                tabTransitionForward = tabBackStack.lastOrNull() !is Screen.History
                tabBackStack.clear()
                tabBackStack.add(Screen.Devices)
                tabBackStack.add(Screen.Types)
            }
            val navigateToHistory = {
                tabTransitionForward = true // always going to index 2 (rightward)
                tabBackStack.clear()
                tabBackStack.add(Screen.Devices)
                tabBackStack.add(Screen.History)
            }

            val onTabSelected: (MainTab) -> Unit = { selectedTab ->
                // Dismiss AI chat overlay whenever switching tabs
                isAiExpanded = false
                when (selectedTab) {
                    MainTab.Devices -> navigateToDevices()
                    MainTab.Types -> navigateToTypes()
                    MainTab.History -> navigateToHistory()
                    MainTab.AI -> {} // AI is an overlay, not a tab
                }
            }

            CompositionLocalProvider(
                LocalAiAvailable provides isAiEnabled,
                LocalAiAction provides { isAiExpanded = true },
            ) {
                Surface(
                    color = MaterialTheme.colorScheme.background,
                ) {
                    // BackHandler priority: AI overlay > detail stack > tab NavDisplay
                    BackHandler(enabled = isAiExpanded) {
                        isAiExpanded = false
                    }
                    val isOnLoginOnly = detailBackStack.size == 1 &&
                        detailBackStack.lastOrNull() is Screen.Login
                    BackHandler(
                        enabled = detailBackStack.isNotEmpty() && !isAiExpanded && !isOnLoginOnly,
                    ) {
                        detailBackStack.removeLastOrNull()
                    }

                    Box(modifier = Modifier.fillMaxSize()) {
                        // LAYER 0: Persistent shell — never animates during tab switches
                        MainScreenShell(
                            currentTab = tabBackStack.lastOrNull().toMainTab(),
                            onTabSelected = onTabSelected,
                            onSettingsClick = { detailBackStack.navigateTo(Screen.Settings) },
                            aiMessages = aiUiMessages,
                            isAiProcessing = isAiProcessing,
                            isAiExpanded = isAiExpanded,
                            onAiExpandedChange = { isAiExpanded = it },
                            onSendAiMessage = onSendAiMessage,
                            onClearAiChat = aiViewModel::clearChat,
                        ) { contentModifier, bottomContentPadding ->

                            // Tab content NavDisplay — only content area animates on tab switch
                            NavDisplay(
                                backStack = tabBackStack,
                                onBack = {
                                    // AI overlay takes priority over tab back-navigation.
                                    // NavDisplay's handler fires before the top-level
                                    // BackHandler when the tab stack has >1 entry.
                                    if (isAiExpanded) {
                                        isAiExpanded = false
                                    } else {
                                        tabBackStack.removeLastOrNull()
                                    }
                                },
                                entryDecorators = listOf(
                                    rememberSaveableStateHolderNavEntryDecorator<Screen>(),
                                    rememberViewModelStoreNavEntryDecorator<Screen>(),
                                ),
                                transitionSpec = {
                                    if (tabTransitionForward) {
                                        slideInHorizontally(tween(300)) { it } + fadeIn(tween(300)) togetherWith
                                            slideOutHorizontally(tween(300)) { -it / 3 } + fadeOut(tween(300))
                                    } else {
                                        slideInHorizontally(tween(300)) { -it / 3 } + fadeIn(tween(300)) togetherWith
                                            slideOutHorizontally(tween(300)) { it } + fadeOut(tween(300))
                                    }
                                },
                                popTransitionSpec = {
                                    slideInHorizontally(tween(300)) { -it / 3 } + fadeIn(tween(300)) togetherWith
                                        slideOutHorizontally(tween(300)) { it } + fadeOut(tween(300))
                                },
                                entryProvider = entryProvider {
                                    entry<Screen.Devices> {
                                        val homeViewModel = viewModel { component.homeViewModel }
                                        DevicesScreenRoot(
                                            viewModel = homeViewModel,
                                            onAddDeviceClick = {
                                                detailBackStack.navigateTo(Screen.AddDevice)
                                            },
                                            onDeviceClick = { deviceId ->
                                                detailBackStack.navigateTo(Screen.DeviceDetail(deviceId))
                                            },
                                            modifier = contentModifier,
                                            contentPadding = bottomContentPadding,
                                        )
                                    }

                                    entry<Screen.Types> {
                                        val deviceTypeListViewModel =
                                            viewModel { component.deviceTypeListViewModel }
                                        TypesScreenRoot(
                                            viewModel = deviceTypeListViewModel,
                                            onAddTypeClick = {
                                                detailBackStack.navigateTo(Screen.AddDeviceType)
                                            },
                                            onTypeClick = { typeId ->
                                                detailBackStack.navigateTo(Screen.DeviceTypeDetail(typeId))
                                            },
                                            modifier = contentModifier,
                                            contentPadding = bottomContentPadding,
                                        )
                                    }

                                    entry<Screen.History> {
                                        val historyListViewModel =
                                            viewModel { component.historyListViewModel }
                                        HistoryScreenRoot(
                                            viewModel = historyListViewModel,
                                            onAddEventClick = {
                                                detailBackStack.navigateTo(Screen.AddBatteryEvent)
                                            },
                                            onEventClick = { eventId, _ ->
                                                detailBackStack.navigateTo(Screen.EventDetail(eventId))
                                            },
                                            modifier = contentModifier,
                                            contentPadding = bottomContentPadding,
                                        )
                                    }
                                },
                            )
                        }

                        // LAYER 1: Detail / modal overlay — full screen, slides over shell
                        if (detailBackStack.isNotEmpty()) {
                            NavDisplay(
                                backStack = detailBackStack,
                                onBack = { detailBackStack.removeLastOrNull() },
                                entryDecorators = listOf(
                                    rememberSaveableStateHolderNavEntryDecorator<Screen>(),
                                    rememberViewModelStoreNavEntryDecorator<Screen>(),
                                ),
                                transitionSpec = {
                                    slideInHorizontally(tween(300)) { it } + fadeIn(tween(300)) togetherWith
                                        slideOutHorizontally(tween(300)) { -it / 3 } + fadeOut(tween(300))
                                },
                                popTransitionSpec = {
                                    slideInHorizontally(tween(300)) { -it / 3 } + fadeIn(tween(300)) togetherWith
                                        slideOutHorizontally(tween(300)) { it } + fadeOut(tween(300))
                                },
                                entryProvider = entryProvider {
                                    entry<Screen.Login> {
                                        LoginScreen(
                                            viewModel = viewModel { component.loginViewModel },
                                            onLoginSuccess = { detailBackStack.clear() },
                                            onSkipLogin = { detailBackStack.clear() },
                                        )
                                    }

                                    entry<Screen.AddDevice>(metadata = slideTransitionMetadata) {
                                        AddDeviceScreen(
                                            viewModel = viewModel { component.addDeviceViewModel },
                                            onDeviceAdded = { detailBackStack.removeLastOrNull() },
                                            onAddDeviceTypeClick = {
                                                detailBackStack.navigateTo(Screen.AddDeviceType)
                                            },
                                            onBack = { detailBackStack.removeLastOrNull() },
                                        )
                                    }

                                    entry<Screen.AddBatteryEvent>(metadata = slideTransitionMetadata) {
                                        AddBatteryEventScreen(
                                            viewModel = viewModel { component.addBatteryEventViewModel },
                                            onEventAdded = { detailBackStack.removeLastOrNull() },
                                            onAddDeviceClick = {
                                                detailBackStack.navigateTo(Screen.AddDevice)
                                            },
                                            onBack = { detailBackStack.removeLastOrNull() },
                                        )
                                    }

                                    entry<Screen.AddDeviceType>(metadata = slideTransitionMetadata) {
                                        AddDeviceTypeScreen(
                                            viewModel = viewModel { component.addDeviceTypeViewModel },
                                            onDeviceTypeAdded = { detailBackStack.removeLastOrNull() },
                                            onBack = { detailBackStack.removeLastOrNull() },
                                        )
                                    }

                                    entry<Screen.DeviceDetail>(metadata = slideTransitionMetadata) {
                                        val args = it
                                        val detailViewModel =
                                            viewModel(key = "DeviceDetail-${args.deviceId}") {
                                                component.deviceDetailViewModelFactory.create(args.deviceId)
                                            }
                                        DeviceDetailScreen(
                                            viewModel = detailViewModel,
                                            onBack = { detailBackStack.removeLastOrNull() },
                                            onEdit = {
                                                detailBackStack.navigateTo(
                                                    Screen.EditDevice(args.deviceId),
                                                )
                                            },
                                            onEventClick = { eventId ->
                                                detailBackStack.navigateTo(Screen.EventDetail(eventId))
                                            },
                                        )
                                    }

                                    entry<Screen.EventDetail>(metadata = slideTransitionMetadata) {
                                        val args = it
                                        val eventViewModel =
                                            viewModel(key = "EventDetail-${args.eventId}") {
                                                component.eventDetailViewModelFactory.create(args.eventId)
                                            }
                                        EventDetailScreen(
                                            viewModel = eventViewModel,
                                            onBack = { detailBackStack.removeLastOrNull() },
                                            onEdit = {
                                                detailBackStack.navigateTo(
                                                    Screen.EditBatteryEvent(args.eventId),
                                                )
                                            },
                                            onDeviceClick = { deviceId ->
                                                detailBackStack.navigateTo(Screen.DeviceDetail(deviceId))
                                            },
                                        )
                                    }

                                    entry<Screen.EditBatteryEvent>(metadata = slideTransitionMetadata) {
                                        val args = it
                                        val editEventViewModel =
                                            viewModel(key = "EditBatteryEvent-${args.eventId}") {
                                                component.editBatteryEventViewModelFactory.create(args.eventId)
                                            }
                                        EditBatteryEventScreen(
                                            viewModel = editEventViewModel,
                                            onBack = { detailBackStack.removeLastOrNull() },
                                            onDelete = {
                                                detailBackStack.removeLastOrNull()
                                                if (detailBackStack.lastOrNull() is Screen.EventDetail) {
                                                    detailBackStack.removeLastOrNull()
                                                }
                                            },
                                        )
                                    }

                                    entry<Screen.EditDevice>(metadata = slideTransitionMetadata) {
                                        val args = it
                                        val editViewModel =
                                            viewModel(key = "EditDevice-${args.deviceId}") {
                                                component.editDeviceViewModelFactory.create(args.deviceId)
                                            }
                                        EditDeviceScreen(
                                            viewModel = editViewModel,
                                            onBack = { detailBackStack.removeLastOrNull() },
                                            onDelete = {
                                                detailBackStack.removeLastOrNull()
                                                if (detailBackStack.lastOrNull() is Screen.DeviceDetail) {
                                                    detailBackStack.removeLastOrNull()
                                                }
                                            },
                                            onAddDeviceTypeClick = {
                                                detailBackStack.navigateTo(Screen.AddDeviceType)
                                            },
                                        )
                                    }

                                    entry<Screen.DeviceTypeDetail>(metadata = slideTransitionMetadata) {
                                        val args = it
                                        val detailViewModel =
                                            viewModel(key = "DeviceTypeDetail-${args.typeId}") {
                                                component.deviceTypeDetailViewModelFactory.create(args.typeId)
                                            }
                                        DeviceTypeDetailScreen(
                                            viewModel = detailViewModel,
                                            onBack = { detailBackStack.removeLastOrNull() },
                                            onEdit = {
                                                detailBackStack.navigateTo(
                                                    Screen.EditDeviceType(args.typeId),
                                                )
                                            },
                                            onDeviceClick = { deviceId ->
                                                detailBackStack.navigateTo(Screen.DeviceDetail(deviceId))
                                            },
                                        )
                                    }

                                    entry<Screen.EditDeviceType>(metadata = slideTransitionMetadata) {
                                        val args = it
                                        val editTypeViewModel =
                                            viewModel(key = "EditDeviceType-${args.typeId}") {
                                                component.editDeviceTypeViewModelFactory.create(args.typeId)
                                            }
                                        EditDeviceTypeScreen(
                                            viewModel = editTypeViewModel,
                                            onBack = { detailBackStack.removeLastOrNull() },
                                            onDelete = {
                                                detailBackStack.removeLastOrNull()
                                                if (detailBackStack.lastOrNull() is Screen.DeviceTypeDetail) {
                                                    detailBackStack.removeLastOrNull()
                                                }
                                            },
                                        )
                                    }

                                    entry<Screen.Settings>(metadata = slideTransitionMetadata) {
                                        SettingsScreen(
                                            viewModel = viewModel { component.settingsViewModel },
                                            onBack = { detailBackStack.removeLastOrNull() },
                                        )
                                    }

                                    entry<Screen.AiChat>(metadata = slideTransitionMetadata) {
                                        AiChatScreen(
                                            viewModel = aiViewModel,
                                            onBack = { detailBackStack.removeLastOrNull() },
                                        )
                                    }
                                },
                            )
                        }
                    }
                }
            } // CompositionLocalProvider
        }
    }
}
