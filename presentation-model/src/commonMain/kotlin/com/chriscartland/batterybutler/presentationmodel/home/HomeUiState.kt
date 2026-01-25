package com.chriscartland.batterybutler.presentationmodel.home

import com.chriscartland.batterybutler.domain.model.Device
import com.chriscartland.batterybutler.domain.model.DeviceType
import com.chriscartland.batterybutler.domain.model.SyncStatus

data class HomeUiState(
    val groups: Map<String, List<Device>> = emptyMap(),
    val devices: Map<String, Device> = emptyMap(), // For lookup if needed
    val groupedDevices: Map<String, List<Device>> = emptyMap(),
    val deviceTypes: Map<String, DeviceType> = emptyMap(),
    val isSortAscending: Boolean = true,
    val isGroupAscending: Boolean = true,
    val sortOption: SortOption = SortOption.NAME,
    val groupOption: GroupOption = GroupOption.NONE,
    val exportData: String? = null,
    val syncStatus: SyncStatus = SyncStatus.Idle,
)

enum class SortOption {
    NAME,
    LOCATION,
    BATTERY_AGE,
    TYPE,
}

enum class GroupOption {
    NONE,
    TYPE,
    LOCATION,
}
