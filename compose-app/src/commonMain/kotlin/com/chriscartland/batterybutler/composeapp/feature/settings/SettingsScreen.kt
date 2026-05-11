package com.chriscartland.batterybutler.composeapp.feature.settings

import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.ui.Modifier
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.chriscartland.batterybutler.presentationcore.util.LocalFileLoader
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
    val currentUser by viewModel.currentUser.collectAsStateWithLifecycle()
    val fileSaver = LocalFileSaver.current
    val fileLoader = LocalFileLoader.current

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
    val aiEngineType by viewModel.aiEngineType.collectAsStateWithLifecycle()
    val currentDatabaseFileName by viewModel.currentDatabaseFileName.collectAsStateWithLifecycle()
    val legacyDatabaseInfo by viewModel.legacyDatabaseInfo.collectAsStateWithLifecycle()
    val restoreInProgress by viewModel.restoreInProgress.collectAsStateWithLifecycle()
    val restoreComplete by viewModel.restoreComplete.collectAsStateWithLifecycle()
    val importResult by viewModel.importResult.collectAsStateWithLifecycle()
    val importError by viewModel.importError.collectAsStateWithLifecycle()
    val importInProgress by viewModel.importInProgress.collectAsStateWithLifecycle()
    SettingsContent(
        networkMode = networkMode,
        availableNetworkModes = viewModel.availableNetworkModes,
        onNetworkModeSelected = viewModel::onNetworkModeSelected,
        aiEngineType = aiEngineType,
        availableAiEngines = viewModel.availableAiEngines,
        onAiEngineSelected = viewModel::onAiEngineSelected,
        onExportData = viewModel::onExportData,
        onImportData = {
            fileLoader.loadFile { bytes ->
                bytes?.let { viewModel.onImportData(it.decodeToString()) }
            }
        },
        importResult = importResult,
        importError = importError,
        importInProgress = importInProgress,
        onImportResultConsumed = viewModel::onImportResultConsumed,
        onBack = onBack,
        appVersion = appVersion,
        currentUser = currentUser,
        onSignOut = viewModel::signOut,
        modifier = modifier,
        currentDatabaseFileName = currentDatabaseFileName,
        legacyDatabaseInfo = legacyDatabaseInfo,
        onRestoreLegacyDatabase = viewModel::onRestoreLegacyDatabase,
        restoreInProgress = restoreInProgress,
        restoreComplete = restoreComplete,
        onRestoreCompleteAcknowledged = viewModel::onRestoreCompleteAcknowledged,
    )
}
