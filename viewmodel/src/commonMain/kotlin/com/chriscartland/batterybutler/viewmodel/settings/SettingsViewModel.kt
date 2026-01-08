package com.chriscartland.batterybutler.viewmodel.settings

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.chriscartland.batterybutler.usecase.ExportDataUseCase
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch
import me.tatarka.inject.annotations.Inject

@Inject
class SettingsViewModel(
    private val exportDataUseCase: ExportDataUseCase,
) : ViewModel() {
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
