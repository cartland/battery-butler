package com.chriscartland.batterybutler.presentation.models.devicetypes

import com.chriscartland.batterybutler.domain.model.DeviceType

sealed interface DeviceTypeListUiState {
    data object Loading : DeviceTypeListUiState

    data class Success(
        val groupedTypes: Map<String, List<DeviceType>> = emptyMap(),
        val sortOption: DeviceTypeSortOption = DeviceTypeSortOption.NAME,
        val groupOption: DeviceTypeGroupOption = DeviceTypeGroupOption.NONE,
        val isSortAscending: Boolean = true,
        val isGroupAscending: Boolean = true,
    ) : DeviceTypeListUiState
}

enum class DeviceTypeSortOption(val label: String) {
    NAME("Name"),
    BATTERY_TYPE("Battery Type"),
}

enum class DeviceTypeGroupOption(val label: String) {
    NONE("None"),
    BATTERY_TYPE("Battery Type"),
}
