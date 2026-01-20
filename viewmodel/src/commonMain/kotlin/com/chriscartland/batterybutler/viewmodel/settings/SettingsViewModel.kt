package com.chriscartland.batterybutler.viewmodel.settings

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.chriscartland.batterybutler.domain.model.NetworkMode
import com.chriscartland.batterybutler.domain.repository.NetworkModeRepository
import com.chriscartland.batterybutler.usecase.ExportDataUseCase
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.launch
import me.tatarka.inject.annotations.Inject

@Inject
class SettingsViewModel(
    private val exportDataUseCase: ExportDataUseCase,
    private val networkModeRepository: NetworkModeRepository,
) : ViewModel() {
    val networkMode: StateFlow<NetworkMode> = networkModeRepository.networkMode
        .stateIn(
            viewModelScope,
            kotlinx.coroutines.flow.SharingStarted
                .WhileSubscribed(5000),
            NetworkMode.MOCK,
        )

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
