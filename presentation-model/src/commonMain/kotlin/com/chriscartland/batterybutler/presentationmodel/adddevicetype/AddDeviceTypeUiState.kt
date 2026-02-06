package com.chriscartland.batterybutler.presentationmodel.adddevicetype

import com.chriscartland.batterybutler.domain.model.BatchOperationResult

/**
 * UI state for the Add Device Type screen.
 *
 * Groups related state to reduce parameter count in composables.
 */
data class AddDeviceTypeUiState(
    val isAiBatchImportEnabled: Boolean = false,
    val aiMessages: List<BatchOperationResult> = emptyList(),
    val suggestedIcon: String? = null,
    val usedIcons: List<String> = emptyList(),
    val isSuggestingIcon: Boolean = false,
)
