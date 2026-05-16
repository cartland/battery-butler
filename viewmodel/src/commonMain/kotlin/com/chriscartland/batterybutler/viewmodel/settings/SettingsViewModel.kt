package com.chriscartland.batterybutler.viewmodel.settings

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.chriscartland.batterybutler.domain.model.AppVersion
import com.chriscartland.batterybutler.domain.model.AuthState
import com.chriscartland.batterybutler.domain.model.DevServerUrl
import com.chriscartland.batterybutler.domain.model.ImportResult
import com.chriscartland.batterybutler.domain.model.LegacyDatabaseInfo
import com.chriscartland.batterybutler.domain.model.NetworkMode
import com.chriscartland.batterybutler.domain.model.ProductionServerUrl
import com.chriscartland.batterybutler.domain.model.RestoreResult
import com.chriscartland.batterybutler.domain.model.Result
import com.chriscartland.batterybutler.domain.model.User
import com.chriscartland.batterybutler.domain.model.ai.AiEngineType
import com.chriscartland.batterybutler.domain.repository.AiPreferencesRepository
import com.chriscartland.batterybutler.domain.repository.AuthRepository
import com.chriscartland.batterybutler.domain.repository.LegacyDatabaseRepository
import com.chriscartland.batterybutler.domain.repository.NetworkModeRepository
import com.chriscartland.batterybutler.domain.repository.RestartCoordinator
import com.chriscartland.batterybutler.usecase.ExportDataUseCase
import com.chriscartland.batterybutler.usecase.GetAppVersionUseCase
import com.chriscartland.batterybutler.usecase.GetLegacyDatabaseInfoUseCase
import com.chriscartland.batterybutler.usecase.ImportDataUseCase
import com.chriscartland.batterybutler.usecase.RestoreLegacyDatabaseUseCase
import com.chriscartland.batterybutler.viewmodel.defaultWhileSubscribed
import com.chriscartland.batterybutler.viewmodel.safeStateIn
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.launch
import me.tatarka.inject.annotations.Inject

@Inject
class SettingsViewModel(
    private val exportDataUseCase: ExportDataUseCase,
    private val importDataUseCase: ImportDataUseCase,
    private val networkModeRepository: NetworkModeRepository,
    private val getAppVersionUseCase: GetAppVersionUseCase,
    private val authRepository: AuthRepository,
    private val aiPreferencesRepository: AiPreferencesRepository,
    private val getLegacyDatabaseInfoUseCase: GetLegacyDatabaseInfoUseCase,
    private val restoreLegacyDatabaseUseCase: RestoreLegacyDatabaseUseCase,
    private val legacyDatabaseRepository: LegacyDatabaseRepository,
    private val restartCoordinator: RestartCoordinator,
    productionServerUrl: ProductionServerUrl,
    devServerUrl: DevServerUrl,
) : ViewModel() {
    val networkMode: StateFlow<NetworkMode> = networkModeRepository.networkMode
        .safeStateIn(
            viewModelScope,
            defaultWhileSubscribed(),
            NetworkMode.None,
        )

    // AWS infrastructure is hibernated — cloud servers are not running.
    // Order: None first (default), then escalating connectivity.
    val availableNetworkModes = listOf(
        NetworkMode.None,
        NetworkMode.Mock,
        NetworkMode.GrpcLocal("http://10.0.2.2:50051"),
        // AWS servers (hibernated — not currently running, kept for future re-enablement)
        NetworkMode.GrpcDev(devServerUrl.url),
        NetworkMode.GrpcAws(productionServerUrl.url),
    )

    val aiEngineType = aiPreferencesRepository.aiEngineType
        .safeStateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), AiEngineType.Cloud)

    val availableAiEngines = AiEngineType.entries

    fun onAiEngineSelected(type: AiEngineType) {
        viewModelScope.launch {
            aiPreferencesRepository.setAiEngineType(type)
        }
    }

    private val _appVersion = MutableStateFlow<AppVersion>(AppVersion.Unavailable)
    val appVersion: StateFlow<AppVersion> = _appVersion.asStateFlow()

    init {
        _appVersion.value = getAppVersionUseCase()
    }

    fun onNetworkModeSelected(mode: NetworkMode) {
        viewModelScope.launch {
            networkModeRepository.setNetworkMode(mode)
        }
    }

    @Suppress("ElseCaseInsteadOfExhaustiveWhen")
    val currentUser: StateFlow<User?> = authRepository.authState
        .map { state ->
            when (state) {
                is AuthState.Authenticated -> state.user
                else -> null // Intentional: unauthenticated states have no user
            }
        }.safeStateIn(viewModelScope, SharingStarted.WhileSubscribed(5_000), null)

    val isSignedIn: StateFlow<Boolean> = authRepository.authState
        .map { it is AuthState.Authenticated }
        .safeStateIn(viewModelScope, SharingStarted.WhileSubscribed(5_000), false)

    fun signOut() {
        viewModelScope.launch {
            authRepository.signOut()
        }
    }

    private val _exportData = MutableStateFlow<String?>(null)
    val exportData: StateFlow<String?> = _exportData.asStateFlow()

    fun onExportData() {
        viewModelScope.launch {
            val data = exportDataUseCase()
            _exportData.value = data
        }
    }

    fun onExportDataConsumed() {
        _exportData.value = null
    }

    // Database info for Settings display
    val currentDatabaseFileName: StateFlow<String> = networkMode
        .map { mode ->
            legacyDatabaseRepository.getCurrentDatabaseFileName(mode)
        }.safeStateIn(viewModelScope, defaultWhileSubscribed(), "")

    val legacyDatabaseInfo: StateFlow<LegacyDatabaseInfo?> = networkMode
        .map { mode ->
            getLegacyDatabaseInfoUseCase(mode)
        }.safeStateIn(viewModelScope, defaultWhileSubscribed(), null)

    private val _restoreInProgress = MutableStateFlow(false)
    val restoreInProgress: StateFlow<Boolean> = _restoreInProgress.asStateFlow()

    private val _restoreComplete = MutableStateFlow(false)
    val restoreComplete: StateFlow<Boolean> = _restoreComplete.asStateFlow()

    /**
     * Detailed outcome of the last restore attempt. Null until a restore has run.
     * UI surfaces a snackbar from this when it transitions to non-null. Cleared by
     * [onRestoreCompleteAcknowledged] alongside the boolean.
     */
    private val _restoreResult = MutableStateFlow<RestoreResult?>(null)
    val restoreResult: StateFlow<RestoreResult?> = _restoreResult.asStateFlow()

    fun onRestoreLegacyDatabase() {
        val info = legacyDatabaseInfo.value ?: return
        viewModelScope.launch {
            _restoreInProgress.value = true
            try {
                val result = restoreLegacyDatabaseUseCase(info.legacyFileName)
                _restoreResult.value = result
                // Preserve the prior "restore completed" signal so existing UI
                // dialogs that only check the boolean still work; the new
                // restoreResult carries the detailed outcome for UI that wants it.
                _restoreComplete.value = result is RestoreResult.Success ||
                    result is RestoreResult.DestructiveFallback
                // Request a process restart for outcomes that require fresh
                // DI / fresh AppDatabase to see restored data (bb-lg42). The
                // emission is buffered on the long-lived AppComponent-scoped
                // RestartCoordinator, so the app-root collector handles it
                // even if the user has already navigated away from Settings.
                if (result is RestoreResult.Success || result is RestoreResult.DestructiveFallback) {
                    restartCoordinator.requestRestart()
                }
            } finally {
                _restoreInProgress.value = false
            }
        }
    }

    fun onRestoreCompleteAcknowledged() {
        _restoreComplete.value = false
        _restoreResult.value = null
    }

    // Import data
    private val _importResult = MutableStateFlow<ImportResult?>(null)
    val importResult: StateFlow<ImportResult?> = _importResult.asStateFlow()

    private val _importError = MutableStateFlow<String?>(null)
    val importError: StateFlow<String?> = _importError.asStateFlow()

    private val _importInProgress = MutableStateFlow(false)
    val importInProgress: StateFlow<Boolean> = _importInProgress.asStateFlow()

    fun onImportData(jsonString: String) {
        if (_importInProgress.value) return
        viewModelScope.launch {
            _importInProgress.value = true
            try {
                when (val result = importDataUseCase(jsonString)) {
                    is Result.Success -> _importResult.value = result.data
                    is Result.Error -> _importError.value = result.error.message
                }
            } finally {
                _importInProgress.value = false
            }
        }
    }

    fun onImportResultConsumed() {
        _importResult.value = null
        _importError.value = null
    }
}
