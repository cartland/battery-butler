package com.chriscartland.batterybutler.viewmodel.settings

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.chriscartland.batterybutler.domain.model.AppVersion
import com.chriscartland.batterybutler.domain.model.NetworkMode
import com.chriscartland.batterybutler.domain.repository.NetworkModeRepository
import com.chriscartland.batterybutler.usecase.ExportDataUseCase
import com.chriscartland.batterybutler.usecase.GetAppVersionUseCase
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.launch
import me.tatarka.inject.annotations.Inject

@Inject
class SettingsViewModel(
    private val exportDataUseCase: ExportDataUseCase,
    private val networkModeRepository: NetworkModeRepository,
    private val getAppVersionUseCase: GetAppVersionUseCase,
) : ViewModel() {
    val networkMode: StateFlow<NetworkMode> = networkModeRepository.networkMode
        .stateIn(
            viewModelScope,
            SharingStarted.WhileSubscribed(5000),
            NetworkMode.Mock,
        )

    val availableNetworkModes = listOf(
        NetworkMode.Mock,
        NetworkMode.GrpcLocal("http://10.0.2.2:50051"), // Hardcoded default for UI list.
        NetworkMode.GrpcAws("http://battery-butler-nlb-847feaa773351518.elb.us-west-1.amazonaws.com:80"),
    )

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
