package com.chriscartland.batterybutler.presentationmodels.home

import com.chriscartland.batterybutler.domain.model.Device
import com.chriscartland.batterybutler.domain.model.DeviceType

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
)

enum class SortOption(
    val label: String,
) {
    NAME("Name"),
    LOCATION("Location"),
    BATTERY_AGE("Battery Age"),
    TYPE("Type"),
}

enum class GroupOption(
    val label: String,
) {
    NONE("None"),
    TYPE("Type"),
    LOCATION("Location"),
}
