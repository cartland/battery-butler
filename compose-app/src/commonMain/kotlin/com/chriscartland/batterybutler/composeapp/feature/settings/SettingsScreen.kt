package com.chriscartland.batterybutler.composeapp.feature.settings

import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.ui.Modifier
import com.chriscartland.batterybutler.presentationcore.util.LocalFileSaver
import com.chriscartland.batterybutler.presentationfeature.settings.SettingsContent
import com.chriscartland.batterybutler.viewmodel.settings.SettingsViewModel
import kotlinx.datetime.TimeZone
import kotlinx.datetime.toLocalDateTime
import kotlin.time.Clock

@Composable
fun SettingsScreen(
    viewModel: SettingsViewModel,
    onBack: () -> Unit,
    modifier: Modifier = Modifier,
) {
    val exportData by viewModel.exportData.collectAsState()
    val fileSaver = LocalFileSaver.current

    LaunchedEffect(exportData) {
        exportData?.let { data ->
            val now = Clock.System.now().toLocalDateTime(TimeZone.currentSystemDefault())
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
    val networkMode by viewModel.networkMode.collectAsState()
    SettingsContent(
        networkMode = networkMode,
        onNetworkModeSelected = viewModel::onNetworkModeSelected,
        onExportData = viewModel::onExportData,
        onBack = onBack,
        modifier = modifier,
    )
}
