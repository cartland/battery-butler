package com.chriscartland.batterybutler.composeapp.feature.main

import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.chriscartland.batterybutler.domain.model.ai.AiRole
import com.chriscartland.batterybutler.presentationcore.util.LocalFileSaver
import com.chriscartland.batterybutler.presentationcore.util.generateFileTimestamp
import com.chriscartland.batterybutler.presentationfeature.aichat.ChatUiMessage
import com.chriscartland.batterybutler.presentationfeature.main.AiScreen
import com.chriscartland.batterybutler.presentationfeature.main.DevicesScreen
import com.chriscartland.batterybutler.presentationfeature.main.HistoryScreen
import com.chriscartland.batterybutler.presentationfeature.main.MainTab
import com.chriscartland.batterybutler.presentationfeature.main.TypesScreen
import com.chriscartland.batterybutler.viewmodel.aichat.AiChatViewModel
import com.chriscartland.batterybutler.viewmodel.devicetypes.DeviceTypeListViewModel
import com.chriscartland.batterybutler.viewmodel.history.HistoryListViewModel
import com.chriscartland.batterybutler.viewmodel.home.HomeViewModel
import kotlinx.datetime.TimeZone
import kotlinx.datetime.toLocalDateTime
import kotlin.time.Clock

@Composable
fun DevicesScreenRoot(
    viewModel: HomeViewModel,
    onTabSelected: (MainTab) -> Unit,
    onSettingsClick: () -> Unit,
    onAddDeviceClick: () -> Unit,
    onDeviceClick: (String) -> Unit,
) {
    val state by viewModel.uiState.collectAsStateWithLifecycle()
    val coreUiState = state
    val fileSaver = LocalFileSaver.current

    // Handle Export Data (Moved from HomeScreen.kt)
    LaunchedEffect(coreUiState.exportData) {
        coreUiState.exportData?.let { data ->
            val now = Clock.System.now().toLocalDateTime(TimeZone.currentSystemDefault())
            val timestamp = generateFileTimestamp(now)
            val filename = "Battery_Butler_Backup_$timestamp.json"
            fileSaver.saveFile(filename, data.encodeToByteArray())
            viewModel.onExportDataConsumed()
        }
    }

    DevicesScreen(
        state = state,
        onTabSelected = onTabSelected,
        onSettingsClick = onSettingsClick,
        onAddDeviceClick = onAddDeviceClick,
        onDeviceClick = onDeviceClick,
        onGroupOptionToggle = { viewModel.toggleGroupDirection() },
        onGroupOptionSelected = { viewModel.onGroupOptionSelected(it) },
        onSortOptionToggle = { viewModel.toggleSortDirection() },
        onSortOptionSelected = { viewModel.onSortOptionSelected(it) },
    )
}

@Composable
fun TypesScreenRoot(
    viewModel: DeviceTypeListViewModel,
    onTabSelected: (MainTab) -> Unit,
    onSettingsClick: () -> Unit,
    onAddTypeClick: () -> Unit,
    onEditType: (String) -> Unit,
) {
    val state by viewModel.uiState.collectAsStateWithLifecycle()

    TypesScreen(
        state = state,
        onTabSelected = onTabSelected,
        onSettingsClick = onSettingsClick,
        onAddTypeClick = onAddTypeClick,
        onEditType = onEditType,
        onPreloadTypes = { viewModel.preloadCommonTypes() },
        onSortOptionSelected = { viewModel.onSortOptionSelected(it) },
        onGroupOptionSelected = { viewModel.onGroupOptionSelected(it) },
        onSortDirectionToggle = { viewModel.toggleSortDirection() },
        onGroupDirectionToggle = { viewModel.toggleGroupDirection() },
    )
}

@Composable
fun HistoryScreenRoot(
    viewModel: HistoryListViewModel,
    onTabSelected: (MainTab) -> Unit,
    onSettingsClick: () -> Unit,
    onAddEventClick: () -> Unit,
    onEventClick: (String, String) -> Unit,
) {
    val state by viewModel.uiState.collectAsStateWithLifecycle()

    HistoryScreen(
        state = state,
        onTabSelected = onTabSelected,
        onSettingsClick = onSettingsClick,
        onAddEventClick = onAddEventClick,
        onEventClick = onEventClick,
    )
}

@Composable
fun AiScreenRoot(
    viewModel: AiChatViewModel,
    onTabSelected: (MainTab) -> Unit,
    onSettingsClick: () -> Unit,
) {
    val messages by viewModel.messages.collectAsStateWithLifecycle()
    val isProcessing by viewModel.isProcessing.collectAsStateWithLifecycle()
    val uiMessages = messages.map { msg ->
        ChatUiMessage(
            id = msg.id,
            text = msg.text,
            isUser = msg.role == AiRole.USER,
        )
    }
    AiScreen(
        messages = uiMessages,
        isProcessing = isProcessing,
        onSendMessage = viewModel::sendMessage,
        onClearChat = viewModel::clearChat,
        onTabSelected = onTabSelected,
        onSettingsClick = onSettingsClick,
    )
}
