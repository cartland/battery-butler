package com.chriscartland.batterybutler.viewmodel.settings

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.chriscartland.batterybutler.domain.model.AppVersion
import com.chriscartland.batterybutler.domain.model.AuthState
import com.chriscartland.batterybutler.domain.model.DevServerUrl
import com.chriscartland.batterybutler.domain.model.NetworkMode
import com.chriscartland.batterybutler.domain.model.ProductionServerUrl
import com.chriscartland.batterybutler.domain.model.User
import com.chriscartland.batterybutler.domain.model.ai.AiEngineType
import com.chriscartland.batterybutler.domain.repository.AiPreferencesRepository
import com.chriscartland.batterybutler.domain.repository.AuthRepository
import com.chriscartland.batterybutler.domain.repository.NetworkModeRepository
import com.chriscartland.batterybutler.usecase.ExportDataUseCase
import com.chriscartland.batterybutler.usecase.GetAppVersionUseCase
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
    private val networkModeRepository: NetworkModeRepository,
    private val getAppVersionUseCase: GetAppVersionUseCase,
    private val authRepository: AuthRepository,
    private val aiPreferencesRepository: AiPreferencesRepository,
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

    val currentUser: StateFlow<User?> = authRepository.authState
        .map { state ->
            when (state) {
                is AuthState.Authenticated -> state.user
                else -> null
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
}
