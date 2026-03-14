package com.chriscartland.batterybutler.presentationmodel.devicetypes

import com.chriscartland.batterybutler.domain.model.DeviceType

sealed interface DeviceTypeListScreenState {
    data object Loading : DeviceTypeListScreenState

    data class Success(
        val groupedTypes: Map<String, List<DeviceType>> = emptyMap(),
        val sortOption: DeviceTypeSortOption = DeviceTypeSortOption.NAME,
        val groupOption: DeviceTypeGroupOption = DeviceTypeGroupOption.NONE,
        val isSortAscending: Boolean = true,
        val isGroupAscending: Boolean = true,
    ) : DeviceTypeListScreenState

    data class Error(
        val message: String,
    ) : DeviceTypeListScreenState
}

enum class DeviceTypeSortOption {
    NAME,
    BATTERY_TYPE,
}

enum class DeviceTypeGroupOption {
    NONE,
    BATTERY_TYPE,
}
