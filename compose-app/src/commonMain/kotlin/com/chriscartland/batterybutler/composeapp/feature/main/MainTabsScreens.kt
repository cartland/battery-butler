package com.chriscartland.batterybutler.composeapp.feature.main

import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.ui.Modifier
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.chriscartland.batterybutler.presentationcore.util.LocalFileSaver
import com.chriscartland.batterybutler.presentationcore.util.generateFileTimestamp
import com.chriscartland.batterybutler.presentationfeature.devicetypes.DeviceTypeListContent
import com.chriscartland.batterybutler.presentationfeature.history.HistoryListContent
import com.chriscartland.batterybutler.presentationfeature.home.HomeScreenContent
import com.chriscartland.batterybutler.viewmodel.devicetypes.DeviceTypeListViewModel
import com.chriscartland.batterybutler.viewmodel.history.HistoryListViewModel
import com.chriscartland.batterybutler.viewmodel.home.HomeViewModel
import kotlinx.datetime.TimeZone
import kotlinx.datetime.toLocalDateTime
import kotlin.time.Clock

@Composable
fun DevicesScreenRoot(
    viewModel: HomeViewModel,
    onAddDeviceClick: () -> Unit,
    onDeviceClick: (String) -> Unit,
    modifier: Modifier = Modifier,
    contentPadding: PaddingValues = PaddingValues(),
) {
    val state by viewModel.uiState.collectAsStateWithLifecycle()
    val isRefreshing by viewModel.isRefreshing.collectAsStateWithLifecycle()
    val fileSaver = LocalFileSaver.current

    // Handle Export Data
    LaunchedEffect(state.exportData) {
        state.exportData?.let { data ->
            val now = Clock.System.now().toLocalDateTime(TimeZone.currentSystemDefault())
            val timestamp = generateFileTimestamp(now)
            val filename = "Battery_Butler_Backup_$timestamp.json"
            fileSaver.saveFile(filename, data.encodeToByteArray())
            viewModel.onExportDataConsumed()
        }
    }

    HomeScreenContent(
        state = state,
        onGroupOptionToggle = { viewModel.toggleGroupDirection() },
        onGroupOptionSelected = { viewModel.onGroupOptionSelected(it) },
        onSortOptionToggle = { viewModel.toggleSortDirection() },
        onSortOptionSelected = { viewModel.onSortOptionSelected(it) },
        onDeviceClick = { onDeviceClick(it.id) },
        onAddDeviceClick = onAddDeviceClick,
        onRetry = { viewModel.retry() },
        modifier = modifier,
        contentPadding = contentPadding,
        isRefreshing = isRefreshing,
        onRefresh = { viewModel.onRefresh() },
    )
}

@Composable
fun TypesScreenRoot(
    viewModel: DeviceTypeListViewModel,
    onAddTypeClick: () -> Unit,
    onTypeClick: (String) -> Unit,
    modifier: Modifier = Modifier,
    contentPadding: PaddingValues = PaddingValues(),
) {
    val state by viewModel.uiState.collectAsStateWithLifecycle()
    val isRefreshing by viewModel.isRefreshing.collectAsStateWithLifecycle()

    DeviceTypeListContent(
        state = state,
        onEditType = onTypeClick,
        onAddTypeClick = onAddTypeClick,
        onPreloadTypes = { viewModel.preloadCommonTypes() },
        onSortOptionSelected = { viewModel.onSortOptionSelected(it) },
        onGroupOptionSelected = { viewModel.onGroupOptionSelected(it) },
        onSortDirectionToggle = { viewModel.toggleSortDirection() },
        onGroupDirectionToggle = { viewModel.toggleGroupDirection() },
        onRetry = { viewModel.retry() },
        modifier = modifier,
        contentPadding = contentPadding,
        isRefreshing = isRefreshing,
        onRefresh = { viewModel.onRefresh() },
    )
}

@Composable
fun HistoryScreenRoot(
    viewModel: HistoryListViewModel,
    onAddEventClick: () -> Unit,
    onEventClick: (String, String) -> Unit,
    modifier: Modifier = Modifier,
    contentPadding: PaddingValues = PaddingValues(),
) {
    val state by viewModel.uiState.collectAsStateWithLifecycle()
    val isRefreshing by viewModel.isRefreshing.collectAsStateWithLifecycle()

    HistoryListContent(
        state = state,
        onEventClick = onEventClick,
        onAddEventClick = onAddEventClick,
        onRetry = { viewModel.retry() },
        modifier = modifier,
        contentPadding = contentPadding,
        isRefreshing = isRefreshing,
        onRefresh = { viewModel.onRefresh() },
    )
}
