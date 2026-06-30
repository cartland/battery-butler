package com.chriscartland.batterybutler.viewmodel.settings

import com.chriscartland.batterybutler.domain.model.AppVersion
import com.chriscartland.batterybutler.domain.model.AuthState
import com.chriscartland.batterybutler.domain.model.DevServerUrl
import com.chriscartland.batterybutler.domain.model.ImportResult
import com.chriscartland.batterybutler.domain.model.LabsProdUrl
import com.chriscartland.batterybutler.domain.model.LabsStagingUrl
import com.chriscartland.batterybutler.domain.model.LegacyDatabaseInfo
import com.chriscartland.batterybutler.domain.model.NetworkMode
import com.chriscartland.batterybutler.domain.model.ProductionServerUrl
import com.chriscartland.batterybutler.domain.model.RestoreResult
import com.chriscartland.batterybutler.domain.model.Result
import com.chriscartland.batterybutler.domain.model.User
import com.chriscartland.batterybutler.domain.model.ai.AiEngineType
import com.chriscartland.batterybutler.domain.repository.AiPreferencesRepository
import com.chriscartland.batterybutler.domain.repository.AuthRepository
import com.chriscartland.batterybutler.domain.repository.LabsAuthRepository
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
import com.rickclephas.kmp.observableviewmodel.MutableStateFlow
import com.rickclephas.kmp.observableviewmodel.ViewModel
import com.rickclephas.kmp.observableviewmodel.coroutineScope
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
    private val labsAuthRepository: LabsAuthRepository,
    private val aiPreferencesRepository: AiPreferencesRepository,
    private val getLegacyDatabaseInfoUseCase: GetLegacyDatabaseInfoUseCase,
    private val restoreLegacyDatabaseUseCase: RestoreLegacyDatabaseUseCase,
    private val legacyDatabaseRepository: LegacyDatabaseRepository,
    private val restartCoordinator: RestartCoordinator,
    productionServerUrl: ProductionServerUrl,
    devServerUrl: DevServerUrl,
    labsStagingUrl: LabsStagingUrl,
    labsProdUrl: LabsProdUrl,
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
        // Labs REST backend (staging + prod). The host is injected from secret config
        // (Workstream E); a blank URL maps to null, rendering the mode unavailable until configured.
        NetworkMode.LabsStaging(labsStagingUrl.url.ifBlank { null }),
        NetworkMode.LabsProd(labsProdUrl.url.ifBlank { null }),
    )

    val aiEngineType = aiPreferencesRepository.aiEngineType
        .safeStateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), AiEngineType.Cloud)

    val availableAiEngines = AiEngineType.entries

    fun onAiEngineSelected(type: AiEngineType) {
        viewModelScope.coroutineScope.launch {
            aiPreferencesRepository.setAiEngineType(type)
        }
    }

    private val _appVersion = MutableStateFlow<AppVersion>(viewModelScope, AppVersion.Unavailable)
    val appVersion: StateFlow<AppVersion> = _appVersion.asStateFlow()

    init {
        _appVersion.value = getAppVersionUseCase()
    }

    fun onNetworkModeSelected(mode: NetworkMode) {
        viewModelScope.coroutineScope.launch {
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
        viewModelScope.coroutineScope.launch {
            authRepository.signOut()
        }
    }

    // --- Labs (REST backend) sign-in -------------------------------------------------------------
    // Separate account from the own-backend auth above: a Labs network mode needs its own Google
    // sign-in against the Labs OAuth client. The UI shows this only when a Labs mode is selected.

    /** True when the selected network mode is a Labs (REST) mode — gates the Labs account UI. */
    val isLabsMode: StateFlow<Boolean> = networkMode
        .map { it is NetworkMode.LabsStaging || it is NetworkMode.LabsProd }
        .safeStateIn(viewModelScope, defaultWhileSubscribed(), false)

    val labsAuthState: StateFlow<AuthState> = labsAuthRepository.labsAuthState
        .safeStateIn(viewModelScope, SharingStarted.WhileSubscribed(5_000), AuthState.Unauthenticated)

    @Suppress("ElseCaseInsteadOfExhaustiveWhen")
    val labsUser: StateFlow<User?> = labsAuthRepository.labsAuthState
        .map { state ->
            when (state) {
                is AuthState.Authenticated -> state.user
                else -> null
            }
        }.safeStateIn(viewModelScope, SharingStarted.WhileSubscribed(5_000), null)

    val isLabsSignedIn: StateFlow<Boolean> = labsAuthRepository.labsAuthState
        .map { it is AuthState.Authenticated }
        .safeStateIn(viewModelScope, SharingStarted.WhileSubscribed(5_000), false)

    val isLabsSigningIn: StateFlow<Boolean> = labsAuthRepository.labsAuthState
        .map { it is AuthState.Authenticating }
        .safeStateIn(viewModelScope, SharingStarted.WhileSubscribed(5_000), false)

    fun onSignInToLabs() {
        viewModelScope.coroutineScope.launch {
            labsAuthRepository.signInToLabs()
        }
    }

    fun onSignOutLabs() {
        viewModelScope.coroutineScope.launch {
            labsAuthRepository.signOutLabs()
        }
    }

    private val _exportData = MutableStateFlow<String?>(viewModelScope, null)
    val exportData: StateFlow<String?> = _exportData.asStateFlow()

    fun onExportData() {
        viewModelScope.coroutineScope.launch {
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

    private val _restoreInProgress = MutableStateFlow(viewModelScope, false)
    val restoreInProgress: StateFlow<Boolean> = _restoreInProgress.asStateFlow()

    private val _restoreComplete = MutableStateFlow(viewModelScope, false)
    val restoreComplete: StateFlow<Boolean> = _restoreComplete.asStateFlow()

    /**
     * Detailed outcome of the last restore attempt. Null until a restore has run.
     * UI surfaces a snackbar from this when it transitions to non-null. Cleared by
     * [onRestoreCompleteAcknowledged] alongside the boolean.
     */
    private val _restoreResult = MutableStateFlow<RestoreResult?>(viewModelScope, null)
    val restoreResult: StateFlow<RestoreResult?> = _restoreResult.asStateFlow()

    fun onRestoreLegacyDatabase() {
        val info = legacyDatabaseInfo.value ?: return
        viewModelScope.coroutineScope.launch {
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
    private val _importResult = MutableStateFlow<ImportResult?>(viewModelScope, null)
    val importResult: StateFlow<ImportResult?> = _importResult.asStateFlow()

    private val _importError = MutableStateFlow<String?>(viewModelScope, null)
    val importError: StateFlow<String?> = _importError.asStateFlow()

    private val _importInProgress = MutableStateFlow(viewModelScope, false)
    val importInProgress: StateFlow<Boolean> = _importInProgress.asStateFlow()

    fun onImportData(jsonString: String) {
        if (_importInProgress.value) return
        viewModelScope.coroutineScope.launch {
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
