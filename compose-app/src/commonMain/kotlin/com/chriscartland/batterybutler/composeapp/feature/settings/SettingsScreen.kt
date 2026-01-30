package com.chriscartland.batterybutler.composeapp.feature.settings

import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.ui.Modifier
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.chriscartland.batterybutler.domain.model.AppVersion
import com.chriscartland.batterybutler.presentationcore.util.LocalFileSaver
import com.chriscartland.batterybutler.presentationcore.util.generateFileTimestamp
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
    val exportData by viewModel.exportData.collectAsStateWithLifecycle()
    val appVersion by viewModel.appVersion.collectAsStateWithLifecycle()
    val fileSaver = LocalFileSaver.current

    LaunchedEffect(exportData) {
        exportData?.let { data ->
            val now = Clock.System.now().toLocalDateTime(TimeZone.currentSystemDefault())
            val timestamp = generateFileTimestamp(now)
            val filename = "Battery_Butler_Backup_$timestamp.json"
            fileSaver.saveFile(filename, data.encodeToByteArray())
            viewModel.onExportDataConsumed()
        }
    }
    val networkMode by viewModel.networkMode.collectAsStateWithLifecycle()
    SettingsContent(
        networkMode = networkMode,
        availableNetworkModes = viewModel.availableNetworkModes,
        onNetworkModeSelected = viewModel::onNetworkModeSelected,
        onExportData = viewModel::onExportData,
        onBack = onBack,
        appVersion = appVersion ?: AppVersion.Desktop("Loading..."),
        modifier = modifier,
    )
}
