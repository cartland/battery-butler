package com.chriscartland.batterybutler.composeapp

import androidx.compose.animation.core.tween
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.animation.slideInHorizontally
import androidx.compose.animation.slideOutHorizontally
import androidx.compose.animation.togetherWith
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
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import androidx.lifecycle.viewmodel.compose.viewModel
import androidx.lifecycle.viewmodel.navigation3.rememberViewModelStoreNavEntryDecorator
import androidx.navigation3.runtime.entryProvider
import androidx.navigation3.runtime.rememberSaveableStateHolderNavEntryDecorator
import androidx.navigation3.ui.NavDisplay
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
import com.chriscartland.batterybutler.composeapp.navigation.isTabScreen
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

@Suppress("ElseCaseInsteadOfExhaustiveWhen")
private fun Screen?.toMainTab(): MainTab =
    when (this) {
        is Screen.Types -> MainTab.Types
        is Screen.History -> MainTab.History
        else -> MainTab.Devices // Intentional: all other screens default to Devices tab
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
            // Unified back stack: Login on top of Devices at launch.
            // Login shows first (full-screen, no chrome). After login,
            // Login is removed and Devices is revealed with shell.
            val backStack = rememberSaveable(saver = ScreenListSaver) {
                mutableStateListOf<Screen>(Screen.Devices, Screen.Login)
            }

            val isAiEnabled = component.featureFlagProvider.isEnabled(FeatureFlag.AI_BATCH_IMPORT)

            // Hoist AI ViewModel at App scope so it persists across tab switches
            val aiViewModel: AiChatViewModel = viewModel { component.aiChatViewModel }
            val aiMessages by aiViewModel.messages.collectAsStateWithLifecycle()
            val isAiProcessing by aiViewModel.isProcessing.collectAsStateWithLifecycle()
            var isAiExpanded by rememberSaveable { mutableStateOf(false) }
            var tabTransitionForward by rememberSaveable { mutableStateOf(true) }
            var isTabTransition by rememberSaveable { mutableStateOf(false) }

            val aiUiMessages = aiMessages.map { msg ->
                ChatUiMessage(
                    id = msg.id,
                    text = msg.text,
                    isUser = msg.role == AiRole.USER,
                )
            }

            // Derive shell visibility and current tab from the unified stack
            val topScreen = backStack.lastOrNull()
            val showChrome = topScreen?.isTabScreen == true
            val currentTab = backStack.lastOrNull { it.isTabScreen }.toMainTab()

            // Determine active tab name for AI context hints
            @Suppress("ElseCaseInsteadOfExhaustiveWhen")
            val currentTabName = when (backStack.lastOrNull { it.isTabScreen }) {
                is Screen.Devices -> MainTab.Devices.name
                is Screen.Types -> MainTab.Types.name
                is Screen.History -> MainTab.History.name
                else -> null // Intentional: non-tab screens have no tab name
            }

            val onSendAiMessage: (String) -> Unit = { text ->
                val hints = buildMap {
                    currentTabName?.let { put("Message sent from", "$it tab") }
                }
                aiViewModel.sendMessage(text, hints = hints)
            }

            // Tab navigation: clear non-tab entries, then replace tab stack
            val navigateToDevices = {
                isAiExpanded = false
                isTabTransition = true
                tabTransitionForward = false
                backStack.clear()
                backStack.add(Screen.Devices)
            }
            val navigateToTypes = {
                isAiExpanded = false
                isTabTransition = true
                tabTransitionForward =
                    backStack.lastOrNull { it.isTabScreen } !is Screen.History
                backStack.clear()
                backStack.add(Screen.Devices)
                backStack.add(Screen.Types)
            }
            val navigateToHistory = {
                isAiExpanded = false
                isTabTransition = true
                tabTransitionForward = true
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

            CompositionLocalProvider(
                LocalAiAvailable provides isAiEnabled,
                LocalAiAction provides { isAiExpanded = true },
            ) {
                Surface(
                    color = MaterialTheme.colorScheme.background,
                ) {
                    MainScreenShell(
                        currentTab = currentTab,
                        showChrome = showChrome,
                        onTabSelected = onTabSelected,
                        onSettingsClick = {
                            isAiExpanded = false
                            isTabTransition = false
                            backStack.navigateTo(Screen.Settings)
                        },
                        aiMessages = aiUiMessages,
                        isAiProcessing = isAiProcessing,
                        isAiExpanded = isAiExpanded,
                        onAiExpandedChange = { isAiExpanded = it },
                        onSendAiMessage = onSendAiMessage,
                        onClearAiChat = aiViewModel::clearChat,
                    ) { contentModifier, bottomContentPadding ->

                        NavDisplay(
                            backStack = backStack,
                            onBack = {
                                val isLoginOnly = backStack.size == 1 &&
                                    backStack.lastOrNull() is Screen.Login
                                when {
                                    isLoginOnly -> { /* no-op */ }
                                    else -> backStack.removeLastOrNull()
                                }
                            },
                            entryDecorators = listOf(
                                rememberSaveableStateHolderNavEntryDecorator<Screen>(),
                                rememberViewModelStoreNavEntryDecorator<Screen>(),
                            ),
                            transitionSpec = {
                                if (isTabTransition) {
                                    // Tab-to-tab: use directional slide
                                    if (tabTransitionForward) {
                                        slideInHorizontally(tween(300)) { it } +
                                            fadeIn(tween(300)) togetherWith
                                            slideOutHorizontally(tween(300)) { -it / 3 } +
                                            fadeOut(tween(300))
                                    } else {
                                        slideInHorizontally(tween(300)) { -it / 3 } +
                                            fadeIn(tween(300)) togetherWith
                                            slideOutHorizontally(tween(300)) { it } +
                                            fadeOut(tween(300))
                                    }
                                } else {
                                    // Detail push: standard right-in slide
                                    slideInHorizontally(tween(300)) { it } +
                                        fadeIn(tween(300)) togetherWith
                                        slideOutHorizontally(tween(300)) { -it / 3 } +
                                        fadeOut(tween(300))
                                }
                            },
                            popTransitionSpec = {
                                slideInHorizontally(tween(300)) { -it / 3 } +
                                    fadeIn(tween(300)) togetherWith
                                    slideOutHorizontally(tween(300)) { it } +
                                    fadeOut(tween(300))
                            },
                            entryProvider = entryProvider {
                                // Tab entries
                                entry<Screen.Devices> {
                                    val homeViewModel =
                                        viewModel { component.homeViewModel }
                                    DevicesScreenRoot(
                                        viewModel = homeViewModel,
                                        onAddDeviceClick = {
                                            isAiExpanded = false
                                            isTabTransition = false
                                            backStack.navigateTo(Screen.AddDevice)
                                        },
                                        onDeviceClick = { deviceId ->
                                            isAiExpanded = false
                                            isTabTransition = false
                                            backStack.navigateTo(
                                                Screen.DeviceDetail(deviceId),
                                            )
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
                                            isAiExpanded = false
                                            isTabTransition = false
                                            backStack.navigateTo(Screen.AddDeviceType)
                                        },
                                        onTypeClick = { typeId ->
                                            isAiExpanded = false
                                            isTabTransition = false
                                            backStack.navigateTo(
                                                Screen.DeviceTypeDetail(typeId),
                                            )
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
                                            isAiExpanded = false
                                            isTabTransition = false
                                            backStack.navigateTo(Screen.AddBatteryEvent)
                                        },
                                        onEventClick = { eventId, _ ->
                                            isAiExpanded = false
                                            isTabTransition = false
                                            backStack.navigateTo(
                                                Screen.EventDetail(eventId),
                                            )
                                        },
                                        modifier = contentModifier,
                                        contentPadding = bottomContentPadding,
                                    )
                                }

                                // Full-screen entries (no chrome)
                                entry<Screen.Login> {
                                    LoginScreen(
                                        viewModel = viewModel { component.loginViewModel },
                                        onLoginSuccess = {
                                            backStack.removeAll { it is Screen.Login }
                                        },
                                        onSkipLogin = {
                                            backStack.removeAll { it is Screen.Login }
                                        },
                                    )
                                }

                                entry<Screen.AddDevice>(
                                    metadata = slideTransitionMetadata,
                                ) {
                                    AddDeviceScreen(
                                        viewModel = viewModel {
                                            component.addDeviceViewModel
                                        },
                                        onDeviceAdded = {
                                            backStack.removeLastOrNull()
                                        },
                                        onAddDeviceTypeClick = {
                                            backStack.navigateTo(Screen.AddDeviceType)
                                        },
                                        onBack = { backStack.removeLastOrNull() },
                                    )
                                }

                                entry<Screen.AddBatteryEvent>(
                                    metadata = slideTransitionMetadata,
                                ) {
                                    AddBatteryEventScreen(
                                        viewModel = viewModel {
                                            component.addBatteryEventViewModel
                                        },
                                        onEventAdded = {
                                            backStack.removeLastOrNull()
                                        },
                                        onAddDeviceClick = {
                                            backStack.navigateTo(Screen.AddDevice)
                                        },
                                        onBack = { backStack.removeLastOrNull() },
                                    )
                                }

                                entry<Screen.AddDeviceType>(
                                    metadata = slideTransitionMetadata,
                                ) {
                                    AddDeviceTypeScreen(
                                        viewModel = viewModel {
                                            component.addDeviceTypeViewModel
                                        },
                                        onDeviceTypeAdded = {
                                            backStack.removeLastOrNull()
                                        },
                                        onBack = { backStack.removeLastOrNull() },
                                    )
                                }

                                entry<Screen.DeviceDetail>(
                                    metadata = slideTransitionMetadata,
                                ) {
                                    val args = it
                                    val detailViewModel = viewModel(
                                        key = "DeviceDetail-${args.deviceId}",
                                    ) {
                                        component.deviceDetailViewModelFactory.create(
                                            args.deviceId,
                                        )
                                    }
                                    DeviceDetailScreen(
                                        viewModel = detailViewModel,
                                        onBack = { backStack.removeLastOrNull() },
                                        onEdit = {
                                            backStack.navigateTo(
                                                Screen.EditDevice(args.deviceId),
                                            )
                                        },
                                        onEventClick = { eventId ->
                                            backStack.navigateTo(
                                                Screen.EventDetail(eventId),
                                            )
                                        },
                                    )
                                }

                                entry<Screen.EventDetail>(
                                    metadata = slideTransitionMetadata,
                                ) {
                                    val args = it
                                    val eventViewModel = viewModel(
                                        key = "EventDetail-${args.eventId}",
                                    ) {
                                        component.eventDetailViewModelFactory.create(
                                            args.eventId,
                                        )
                                    }
                                    EventDetailScreen(
                                        viewModel = eventViewModel,
                                        onBack = { backStack.removeLastOrNull() },
                                        onEdit = {
                                            backStack.navigateTo(
                                                Screen.EditBatteryEvent(args.eventId),
                                            )
                                        },
                                        onDeviceClick = { deviceId ->
                                            backStack.navigateTo(
                                                Screen.DeviceDetail(deviceId),
                                            )
                                        },
                                    )
                                }

                                entry<Screen.EditBatteryEvent>(
                                    metadata = slideTransitionMetadata,
                                ) {
                                    val args = it
                                    val editEventViewModel = viewModel(
                                        key = "EditBatteryEvent-${args.eventId}",
                                    ) {
                                        component.editBatteryEventViewModelFactory.create(
                                            args.eventId,
                                        )
                                    }
                                    EditBatteryEventScreen(
                                        viewModel = editEventViewModel,
                                        onBack = { backStack.removeLastOrNull() },
                                        onDelete = {
                                            backStack.removeLastOrNull()
                                            if (backStack.lastOrNull() is Screen.EventDetail) {
                                                backStack.removeLastOrNull()
                                            }
                                        },
                                    )
                                }

                                entry<Screen.EditDevice>(
                                    metadata = slideTransitionMetadata,
                                ) {
                                    val args = it
                                    val editViewModel = viewModel(
                                        key = "EditDevice-${args.deviceId}",
                                    ) {
                                        component.editDeviceViewModelFactory.create(
                                            args.deviceId,
                                        )
                                    }
                                    EditDeviceScreen(
                                        viewModel = editViewModel,
                                        onBack = { backStack.removeLastOrNull() },
                                        onDelete = {
                                            backStack.removeLastOrNull()
                                            if (backStack.lastOrNull() is Screen.DeviceDetail) {
                                                backStack.removeLastOrNull()
                                            }
                                        },
                                        onAddDeviceTypeClick = {
                                            backStack.navigateTo(Screen.AddDeviceType)
                                        },
                                    )
                                }

                                entry<Screen.DeviceTypeDetail>(
                                    metadata = slideTransitionMetadata,
                                ) {
                                    val args = it
                                    val detailViewModel = viewModel(
                                        key = "DeviceTypeDetail-${args.typeId}",
                                    ) {
                                        component.deviceTypeDetailViewModelFactory.create(
                                            args.typeId,
                                        )
                                    }
                                    DeviceTypeDetailScreen(
                                        viewModel = detailViewModel,
                                        onBack = { backStack.removeLastOrNull() },
                                        onEdit = {
                                            backStack.navigateTo(
                                                Screen.EditDeviceType(args.typeId),
                                            )
                                        },
                                        onDeviceClick = { deviceId ->
                                            backStack.navigateTo(
                                                Screen.DeviceDetail(deviceId),
                                            )
                                        },
                                    )
                                }

                                entry<Screen.EditDeviceType>(
                                    metadata = slideTransitionMetadata,
                                ) {
                                    val args = it
                                    val editTypeViewModel = viewModel(
                                        key = "EditDeviceType-${args.typeId}",
                                    ) {
                                        component.editDeviceTypeViewModelFactory.create(
                                            args.typeId,
                                        )
                                    }
                                    EditDeviceTypeScreen(
                                        viewModel = editTypeViewModel,
                                        onBack = { backStack.removeLastOrNull() },
                                        onDelete = {
                                            backStack.removeLastOrNull()
                                            if (backStack.lastOrNull() is Screen.DeviceTypeDetail) {
                                                backStack.removeLastOrNull()
                                            }
                                        },
                                    )
                                }

                                entry<Screen.Settings>(
                                    metadata = slideTransitionMetadata,
                                ) {
                                    SettingsScreen(
                                        viewModel = viewModel {
                                            component.settingsViewModel
                                        },
                                        onBack = { backStack.removeLastOrNull() },
                                    )
                                }

                                entry<Screen.AiChat>(
                                    metadata = slideTransitionMetadata,
                                ) {
                                    AiChatScreen(
                                        viewModel = aiViewModel,
                                        onBack = { backStack.removeLastOrNull() },
                                    )
                                }
                            },
                        )
                    }
                }
            } // CompositionLocalProvider
        }
    }
}
