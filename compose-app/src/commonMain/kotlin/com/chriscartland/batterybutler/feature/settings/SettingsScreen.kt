package com.chriscartland.batterybutler.feature.settings

import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.ui.Modifier
import com.chriscartland.batterybutler.presenter.feature.settings.SettingsContent
import com.chriscartland.batterybutler.presenter.core.util.LocalFileSaver
import com.chriscartland.batterybutler.viewmodel.settings.SettingsViewModel

@Composable
fun SettingsScreen(
    viewModel: SettingsViewModel,
    onBack: () -> Unit,
    modifier: Modifier = Modifier,
) {
    val exportData by viewModel.exportData.collectAsState()
    val fileSaver = LocalFileSaver.current

    androidx.compose.runtime.LaunchedEffect(exportData) {
        exportData?.let { data ->
            fileSaver.saveFile("battery_butler_data.json", data.encodeToByteArray())
            viewModel.onExportDataConsumed()
        }
    }
    SettingsContent(
        onExportData = viewModel::onExportData,
        onBack = onBack,
        modifier = modifier,
    )
}
