package com.chriscartland.batterybutler.feature.home

import androidx.compose.foundation.ExperimentalFoundationApi
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import com.chriscartland.batterybutler.ui.feature.home.HomeScreenContent
import com.chriscartland.batterybutler.ui.util.LocalShareHandler
import com.chriscartland.batterybutler.viewmodel.home.HomeViewModel

@OptIn(ExperimentalMaterial3Api::class, ExperimentalFoundationApi::class)
@Composable
fun HomeScreen(
    viewModel: HomeViewModel,
    onAddDeviceClick: () -> Unit,
    onDeviceClick: (String) -> Unit,
    onManageTypesClick: () -> Unit,
    modifier: Modifier = Modifier,
) {
    val state by viewModel.uiState.collectAsState()

    val coreUiState = state

    val shareHandler = LocalShareHandler.current
    // Handle Export Data
    androidx.compose.runtime.LaunchedEffect(coreUiState.exportData) {
        coreUiState.exportData?.let { data ->
            shareHandler.shareText(data)
            viewModel.onExportDataConsumed()
        }
    }

    HomeScreenContent(
        state = state,
        onGroupOptionToggle = { viewModel.toggleGroupDirection() },
        onGroupOptionSelected = { viewModel.onGroupOptionSelected(it) },
        onSortOptionToggle = { viewModel.toggleSortDirection() },
        onSortOptionSelected = { viewModel.onSortOptionSelected(it) },
        onDeviceClick = onDeviceClick,
        modifier = modifier,
    )
}
