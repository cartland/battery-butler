package com.chriscartland.batterybutler.presentationfeature.main

import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.slideInVertically
import androidx.compose.animation.slideOutVertically
import androidx.compose.foundation.clickable
import androidx.compose.foundation.interaction.MutableInteractionSource
import androidx.compose.foundation.interaction.collectIsPressedAsState
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.calculateEndPadding
import androidx.compose.foundation.layout.calculateStartPadding
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.width
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.List
import androidx.compose.material.icons.filled.AutoAwesome
import androidx.compose.material.icons.filled.Delete
import androidx.compose.material.icons.filled.History
import androidx.compose.material.icons.filled.Home
import androidx.compose.material.icons.filled.KeyboardArrowDown
import androidx.compose.material.icons.filled.Settings
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.NavigationBar
import androidx.compose.material3.NavigationBarItem
import androidx.compose.material3.NavigationBarItemDefaults
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.CompositionLocalProvider
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberUpdatedState
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.platform.LocalLayoutDirection
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.tooling.preview.Preview
import com.chriscartland.batterybutler.composeresources.composeStringResource
import com.chriscartland.batterybutler.composeresources.generated.resources.Res
import com.chriscartland.batterybutler.composeresources.generated.resources.tab_ai
import com.chriscartland.batterybutler.composeresources.generated.resources.tab_devices
import com.chriscartland.batterybutler.composeresources.generated.resources.tab_history
import com.chriscartland.batterybutler.composeresources.generated.resources.tab_types
import com.chriscartland.batterybutler.domain.model.BatteryEvent
import com.chriscartland.batterybutler.domain.model.Device
import com.chriscartland.batterybutler.domain.model.DeviceType
import com.chriscartland.batterybutler.presentationcore.components.ButlerCenteredTopAppBar
import com.chriscartland.batterybutler.presentationcore.theme.BatteryButlerTheme
import com.chriscartland.batterybutler.presentationcore.theme.LocalAiAvailable
import com.chriscartland.batterybutler.presentationcore.theme.Padding
import com.chriscartland.batterybutler.presentationfeature.aichat.AiTabContent
import com.chriscartland.batterybutler.presentationfeature.aichat.ChatUiMessage
import com.chriscartland.batterybutler.presentationfeature.devicetypes.DeviceTypeListContent
import com.chriscartland.batterybutler.presentationfeature.history.HistoryListContent
import com.chriscartland.batterybutler.presentationfeature.home.HomeScreenContent
import com.chriscartland.batterybutler.presentationmodel.devicetypes.DeviceTypeGroupOption
import com.chriscartland.batterybutler.presentationmodel.devicetypes.DeviceTypeListUiState
import com.chriscartland.batterybutler.presentationmodel.devicetypes.DeviceTypeSortOption
import com.chriscartland.batterybutler.presentationmodel.history.HistoryItemUiModel
import com.chriscartland.batterybutler.presentationmodel.history.HistoryListUiState
import com.chriscartland.batterybutler.presentationmodel.home.GroupOption
import com.chriscartland.batterybutler.presentationmodel.home.HomeUiState
import com.chriscartland.batterybutler.presentationmodel.home.SortOption
import kotlinx.serialization.Serializable
import org.jetbrains.compose.resources.StringResource
import kotlin.time.Clock
import kotlin.time.ExperimentalTime
import kotlin.time.Instant

@Serializable
enum class MainTab {
    Devices,
    Types,
    History,
    AI,
}

fun MainTab.labelRes(): StringResource =
    when (this) {
        MainTab.Devices -> Res.string.tab_devices
        MainTab.Types -> Res.string.tab_types
        MainTab.History -> Res.string.tab_history
        MainTab.AI -> Res.string.tab_ai
    }

fun MainTab.icon(): ImageVector =
    when (this) {
        MainTab.Devices -> Icons.Default.Home
        MainTab.Types -> Icons.AutoMirrored.Filled.List
        MainTab.History -> Icons.Default.History
        MainTab.AI -> Icons.Default.AutoAwesome
    }

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun MainScreenShell(
    currentTab: MainTab,
    onTabSelected: (MainTab) -> Unit,
    onSettingsClick: () -> Unit,
    aiMessages: List<ChatUiMessage>,
    isAiProcessing: Boolean,
    isAiExpanded: Boolean,
    onAiExpandedChange: (Boolean) -> Unit,
    onSendAiMessage: (String) -> Unit,
    onClearAiChat: () -> Unit,
    modifier: Modifier = Modifier,
    content: @Composable (Modifier, PaddingValues) -> Unit,
) {
    val isAiAvailable = LocalAiAvailable.current
    val visibleTabs = MainTab.entries.filter { it != MainTab.AI }

    Scaffold(
        modifier = modifier,
        topBar = {
            Box(
                modifier = if (isAiExpanded) {
                    Modifier.clickable { onAiExpandedChange(false) }
                } else {
                    Modifier
                },
            ) {
                ButlerCenteredTopAppBar(
                    title = composeStringResource(currentTab.labelRes()),
                    actions = {
                        IconButton(onClick = onSettingsClick) {
                            Icon(
                                imageVector = Icons.Default.Settings,
                                contentDescription = "Settings",
                            )
                        }
                    },
                )
            }
        },
        bottomBar = {
            Column {
                if (!isAiExpanded && isAiAvailable) {
                    val currentOnAiExpandedChange by rememberUpdatedState(onAiExpandedChange)
                    val interactionSource = remember { MutableInteractionSource() }
                    val isPressed = interactionSource.collectIsPressedAsState()
                    LaunchedEffect(isPressed.value) {
                        if (isPressed.value) currentOnAiExpandedChange(true)
                    }
                    OutlinedTextField(
                        value = "",
                        onValueChange = {},
                        readOnly = true,
                        placeholder = { Text("Ask AI...") },
                        leadingIcon = {
                            Icon(
                                imageVector = Icons.Default.AutoAwesome,
                                contentDescription = null,
                            )
                        },
                        singleLine = true,
                        interactionSource = interactionSource,
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(
                                horizontal = Padding.standard,
                                vertical = Padding.small,
                            ),
                    )
                }
                NavigationBar {
                    visibleTabs.forEach { tab ->
                        NavigationBarItem(
                            selected = currentTab == tab,
                            onClick = {
                                if (isAiExpanded) onAiExpandedChange(false)
                                onTabSelected(tab)
                            },
                            icon = {
                                Icon(
                                    tab.icon(),
                                    contentDescription = composeStringResource(tab.labelRes()),
                                )
                            },
                            label = { Text(composeStringResource(tab.labelRes())) },
                            colors = NavigationBarItemDefaults.colors(
                                selectedIconColor = MaterialTheme.colorScheme.primary,
                                selectedTextColor = MaterialTheme.colorScheme.primary,
                                unselectedIconColor = MaterialTheme.colorScheme.onSurfaceVariant,
                                unselectedTextColor = MaterialTheme.colorScheme.onSurfaceVariant,
                            ),
                            modifier = Modifier.testTag("BottomNav_${tab.name}"),
                        )
                    }
                }
            }
        },
        content = { innerPadding ->
            val layoutDirection = LocalLayoutDirection.current
            val contentModifier = Modifier.padding(
                top = innerPadding.calculateTopPadding() + Padding.standard,
                start = innerPadding.calculateStartPadding(layoutDirection),
                end = innerPadding.calculateEndPadding(layoutDirection),
            )
            val bottomContentPadding = PaddingValues(
                bottom = innerPadding.calculateBottomPadding() + Padding.standard,
            )

            Box(modifier = Modifier.fillMaxSize()) {
                content(contentModifier, bottomContentPadding)

                AnimatedVisibility(
                    visible = isAiExpanded,
                    enter = slideInVertically(initialOffsetY = { it }),
                    exit = slideOutVertically(targetOffsetY = { it }),
                ) {
                    Surface(
                        modifier = Modifier
                            .fillMaxSize()
                            .padding(
                                top = innerPadding.calculateTopPadding(),
                                bottom = innerPadding.calculateBottomPadding(),
                            ),
                        color = MaterialTheme.colorScheme.surface,
                    ) {
                        Column(modifier = Modifier.fillMaxSize()) {
                            Row(
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .padding(
                                        start = Padding.standard,
                                        end = Padding.small,
                                    ),
                                verticalAlignment = Alignment.CenterVertically,
                            ) {
                                Icon(
                                    imageVector = Icons.Default.AutoAwesome,
                                    contentDescription = null,
                                    tint = MaterialTheme.colorScheme.primary,
                                )
                                Spacer(modifier = Modifier.width(Padding.small))
                                Text(
                                    text = "AI Chat",
                                    style = MaterialTheme.typography.titleMedium,
                                    modifier = Modifier.weight(1f),
                                )
                                if (aiMessages.isNotEmpty()) {
                                    IconButton(onClick = onClearAiChat) {
                                        Icon(
                                            imageVector = Icons.Default.Delete,
                                            contentDescription = "Clear chat",
                                        )
                                    }
                                }
                                IconButton(
                                    onClick = { onAiExpandedChange(false) },
                                ) {
                                    Icon(
                                        imageVector = Icons.Default.KeyboardArrowDown,
                                        contentDescription = "Collapse AI chat",
                                    )
                                }
                            }
                            AiTabContent(
                                messages = aiMessages,
                                isProcessing = isAiProcessing,
                                onSendMessage = onSendAiMessage,
                                modifier = Modifier.weight(1f),
                            )
                        }
                    }
                }
            }
        },
    )
}

@OptIn(ExperimentalTime::class)
@Composable
fun DevicesScreen(
    state: HomeUiState,
    onTabSelected: (MainTab) -> Unit,
    onSettingsClick: () -> Unit,
    onAddDeviceClick: () -> Unit,
    onDeviceClick: (String) -> Unit,
    onGroupOptionToggle: () -> Unit,
    onGroupOptionSelected: (GroupOption) -> Unit,
    onSortOptionToggle: () -> Unit,
    onSortOptionSelected: (SortOption) -> Unit,
    aiMessages: List<ChatUiMessage>,
    isAiProcessing: Boolean,
    isAiExpanded: Boolean,
    onAiExpandedChange: (Boolean) -> Unit,
    onSendAiMessage: (String) -> Unit,
    onClearAiChat: () -> Unit,
    nowInstant: Instant = Clock.System.now(),
) {
    MainScreenShell(
        currentTab = MainTab.Devices,
        onTabSelected = onTabSelected,
        onSettingsClick = onSettingsClick,
        aiMessages = aiMessages,
        isAiProcessing = isAiProcessing,
        isAiExpanded = isAiExpanded,
        onAiExpandedChange = onAiExpandedChange,
        onSendAiMessage = onSendAiMessage,
        onClearAiChat = onClearAiChat,
    ) { contentModifier, bottomContentPadding ->
        HomeScreenContent(
            state = state,
            onGroupOptionToggle = onGroupOptionToggle,
            onGroupOptionSelected = onGroupOptionSelected,
            onSortOptionToggle = onSortOptionToggle,
            onSortOptionSelected = onSortOptionSelected,
            onDeviceClick = { onDeviceClick(it.id) },
            onAddDeviceClick = onAddDeviceClick,
            modifier = contentModifier,
            contentPadding = bottomContentPadding,
            nowInstant = nowInstant,
        )
    }
}

@Composable
fun TypesScreen(
    state: DeviceTypeListUiState,
    onTabSelected: (MainTab) -> Unit,
    onSettingsClick: () -> Unit,
    onAddTypeClick: () -> Unit,
    onEditType: (String) -> Unit,
    onPreloadTypes: () -> Unit,
    onSortOptionSelected: (DeviceTypeSortOption) -> Unit,
    onGroupOptionSelected: (DeviceTypeGroupOption) -> Unit,
    onSortDirectionToggle: () -> Unit,
    onGroupDirectionToggle: () -> Unit,
    aiMessages: List<ChatUiMessage>,
    isAiProcessing: Boolean,
    isAiExpanded: Boolean,
    onAiExpandedChange: (Boolean) -> Unit,
    onSendAiMessage: (String) -> Unit,
    onClearAiChat: () -> Unit,
) {
    MainScreenShell(
        currentTab = MainTab.Types,
        onTabSelected = onTabSelected,
        onSettingsClick = onSettingsClick,
        aiMessages = aiMessages,
        isAiProcessing = isAiProcessing,
        isAiExpanded = isAiExpanded,
        onAiExpandedChange = onAiExpandedChange,
        onSendAiMessage = onSendAiMessage,
        onClearAiChat = onClearAiChat,
    ) { contentModifier, bottomContentPadding ->
        DeviceTypeListContent(
            state = state,
            onEditType = onEditType,
            onAddTypeClick = onAddTypeClick,
            onPreloadTypes = onPreloadTypes,
            onSortOptionSelected = onSortOptionSelected,
            onGroupOptionSelected = onGroupOptionSelected,
            onSortDirectionToggle = onSortDirectionToggle,
            onGroupDirectionToggle = onGroupDirectionToggle,
            modifier = contentModifier,
            contentPadding = bottomContentPadding,
        )
    }
}

@OptIn(ExperimentalTime::class)
@Composable
fun HistoryScreen(
    state: HistoryListUiState,
    onTabSelected: (MainTab) -> Unit,
    onSettingsClick: () -> Unit,
    onAddEventClick: () -> Unit,
    onEventClick: (String, String) -> Unit,
    aiMessages: List<ChatUiMessage>,
    isAiProcessing: Boolean,
    isAiExpanded: Boolean,
    onAiExpandedChange: (Boolean) -> Unit,
    onSendAiMessage: (String) -> Unit,
    onClearAiChat: () -> Unit,
    nowInstant: Instant = Clock.System.now(),
) {
    MainScreenShell(
        currentTab = MainTab.History,
        onTabSelected = onTabSelected,
        onSettingsClick = onSettingsClick,
        aiMessages = aiMessages,
        isAiProcessing = isAiProcessing,
        isAiExpanded = isAiExpanded,
        onAiExpandedChange = onAiExpandedChange,
        onSendAiMessage = onSendAiMessage,
        onClearAiChat = onClearAiChat,
    ) { contentModifier, bottomContentPadding ->
        HistoryListContent(
            state = state,
            onEventClick = onEventClick,
            onAddEventClick = onAddEventClick,
            modifier = contentModifier,
            contentPadding = bottomContentPadding,
            nowInstant = nowInstant,
        )
    }
}

@Preview(showBackground = true)
@Composable
fun DevicesScreenPreview() {
    BatteryButlerTheme {
        CompositionLocalProvider(LocalAiAvailable provides true) {
            val now = Instant.parse("2026-01-18T17:00:00Z")
            val type = DeviceType("type1", "Smoke Alarm", "detector_smoke")
            val device = Device("dev1", "Kitchen Smoke", "type1", now, now, "Kitchen")
            val state = HomeUiState(
                groupedDevices = mapOf("All" to listOf(device)),
                deviceTypes = mapOf("type1" to type),
            )
            DevicesScreen(
                state = state,
                onTabSelected = {},
                onSettingsClick = {},
                onAddDeviceClick = {},
                onDeviceClick = {},
                onGroupOptionToggle = {},
                onGroupOptionSelected = {},
                onSortOptionToggle = {},
                onSortOptionSelected = {},
                aiMessages = emptyList(),
                isAiProcessing = false,
                isAiExpanded = false,
                onAiExpandedChange = {},
                onSendAiMessage = {},
                onClearAiChat = {},
                nowInstant = now,
            )
        }
    }
}

@Preview(showBackground = true)
@Composable
fun TypesScreenPreview() {
    BatteryButlerTheme {
        CompositionLocalProvider(LocalAiAvailable provides true) {
            val type = DeviceType("type1", "Smoke Alarm", "detector_smoke")
            val state = DeviceTypeListUiState.Success(
                groupedTypes = mapOf("All" to listOf(type)),
            )
            TypesScreen(
                state = state,
                onTabSelected = {},
                onSettingsClick = {},
                onAddTypeClick = {},
                onEditType = {},
                onPreloadTypes = {},
                onSortOptionSelected = {},
                onGroupOptionSelected = {},
                onSortDirectionToggle = {},
                onGroupDirectionToggle = {},
                aiMessages = emptyList(),
                isAiProcessing = false,
                isAiExpanded = false,
                onAiExpandedChange = {},
                onSendAiMessage = {},
                onClearAiChat = {},
            )
        }
    }
}

@OptIn(ExperimentalTime::class)
@Preview(showBackground = true)
@Composable
fun HistoryScreenPreview() {
    BatteryButlerTheme {
        CompositionLocalProvider(LocalAiAvailable provides true) {
            val nowInstant = Instant.parse("2026-01-18T17:00:00Z")
            val eventInstant = Instant.parse("2026-01-11T17:00:00Z") // 7 days ago
            val event = BatteryEvent("evt1", "dev1", eventInstant)
            val item = HistoryItemUiModel(event, "Kitchen Smoke", "Smoke Alarm", "Kitchen")
            val state = HistoryListUiState.Success(
                items = listOf(item),
            )
            HistoryScreen(
                state = state,
                onTabSelected = {},
                onSettingsClick = {},
                onAddEventClick = {},
                onEventClick = { _, _ -> },
                aiMessages = emptyList(),
                isAiProcessing = false,
                isAiExpanded = false,
                onAiExpandedChange = {},
                onSendAiMessage = {},
                onClearAiChat = {},
                nowInstant = nowInstant,
            )
        }
    }
}

@Preview(showBackground = true)
@Composable
fun AiBarCollapsedDevicesPreview() {
    BatteryButlerTheme {
        CompositionLocalProvider(LocalAiAvailable provides true) {
            val now = Instant.parse("2026-01-18T17:00:00Z")
            val type = DeviceType("type1", "Smoke Alarm", "detector_smoke")
            val device = Device("dev1", "Kitchen Smoke", "type1", now, now, "Kitchen")
            val state = HomeUiState(
                groupedDevices = mapOf("All" to listOf(device)),
                deviceTypes = mapOf("type1" to type),
            )
            DevicesScreen(
                state = state,
                onTabSelected = {},
                onSettingsClick = {},
                onAddDeviceClick = {},
                onDeviceClick = {},
                onGroupOptionToggle = {},
                onGroupOptionSelected = {},
                onSortOptionToggle = {},
                onSortOptionSelected = {},
                aiMessages = emptyList(),
                isAiProcessing = false,
                isAiExpanded = false,
                onAiExpandedChange = {},
                onSendAiMessage = {},
                onClearAiChat = {},
                nowInstant = now,
            )
        }
    }
}

@Preview(showBackground = true)
@Composable
fun AiBarCollapsedTypesPreview() {
    BatteryButlerTheme {
        CompositionLocalProvider(LocalAiAvailable provides true) {
            val type = DeviceType("type1", "Smoke Alarm", "detector_smoke")
            val state = DeviceTypeListUiState.Success(
                groupedTypes = mapOf("All" to listOf(type)),
            )
            TypesScreen(
                state = state,
                onTabSelected = {},
                onSettingsClick = {},
                onAddTypeClick = {},
                onEditType = {},
                onPreloadTypes = {},
                onSortOptionSelected = {},
                onGroupOptionSelected = {},
                onSortDirectionToggle = {},
                onGroupDirectionToggle = {},
                aiMessages = emptyList(),
                isAiProcessing = false,
                isAiExpanded = false,
                onAiExpandedChange = {},
                onSendAiMessage = {},
                onClearAiChat = {},
            )
        }
    }
}

@OptIn(ExperimentalTime::class)
@Preview(showBackground = true)
@Composable
fun AiBarCollapsedHistoryPreview() {
    BatteryButlerTheme {
        CompositionLocalProvider(LocalAiAvailable provides true) {
            val nowInstant = Instant.parse("2026-01-18T17:00:00Z")
            val eventInstant = Instant.parse("2026-01-11T17:00:00Z")
            val event = BatteryEvent("evt1", "dev1", eventInstant)
            val item = HistoryItemUiModel(event, "Kitchen Smoke", "Smoke Alarm", "Kitchen")
            val state = HistoryListUiState.Success(
                items = listOf(item),
            )
            HistoryScreen(
                state = state,
                onTabSelected = {},
                onSettingsClick = {},
                onAddEventClick = {},
                onEventClick = { _, _ -> },
                aiMessages = emptyList(),
                isAiProcessing = false,
                isAiExpanded = false,
                onAiExpandedChange = {},
                onSendAiMessage = {},
                onClearAiChat = {},
                nowInstant = nowInstant,
            )
        }
    }
}

@Preview(showBackground = true)
@Composable
fun AiOverlayExpandedPreview() {
    BatteryButlerTheme {
        CompositionLocalProvider(LocalAiAvailable provides true) {
            val now = Instant.parse("2026-01-18T17:00:00Z")
            val type = DeviceType("type1", "Smoke Alarm", "detector_smoke")
            val device = Device("dev1", "Kitchen Smoke", "type1", now, now, "Kitchen")
            val state = HomeUiState(
                groupedDevices = mapOf("All" to listOf(device)),
                deviceTypes = mapOf("type1" to type),
            )
            DevicesScreen(
                state = state,
                onTabSelected = {},
                onSettingsClick = {},
                onAddDeviceClick = {},
                onDeviceClick = {},
                onGroupOptionToggle = {},
                onGroupOptionSelected = {},
                onSortOptionToggle = {},
                onSortOptionSelected = {},
                aiMessages = listOf(
                    ChatUiMessage("1", "Add a smoke detector in the kitchen", isUser = true),
                    ChatUiMessage(
                        "2",
                        "I've added a smoke detector device in the kitchen for you.",
                        isUser = false,
                    ),
                ),
                isAiProcessing = false,
                isAiExpanded = true,
                onAiExpandedChange = {},
                onSendAiMessage = {},
                onClearAiChat = {},
                nowInstant = now,
            )
        }
    }
}
