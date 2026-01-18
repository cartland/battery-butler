package com.chriscartland.batterybutler.composeapp.feature.home

import androidx.compose.foundation.ExperimentalFoundationApi
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import com.chriscartland.batterybutler.domain.util.SystemTime
import com.chriscartland.batterybutler.presentationcore.util.LocalFileSaver
import com.chriscartland.batterybutler.presentationfeature.home.HomeScreenContent
import com.chriscartland.batterybutler.viewmodel.home.HomeViewModel
import kotlinx.datetime.TimeZone
import kotlinx.datetime.toLocalDateTime

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

    val fileSaver = LocalFileSaver.current
    // Handle Export Data
    LaunchedEffect(coreUiState.exportData) {
        coreUiState.exportData?.let { data ->
            val now = SystemTime.now().toLocalDateTime(TimeZone.currentSystemDefault())
            val timestamp = "${now.year}_${now.monthNumber.toString().padStart(
                2,
                '0',
            )}_${now.dayOfMonth.toString().padStart(
                2,
                '0',
            )}_${now.hour.toString().padStart(2, '0')}_${now.minute.toString().padStart(2, '0')}_${now.second.toString().padStart(2, '0')}"
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
        onDeviceClick = onDeviceClick,
        modifier = modifier,
    )
}
