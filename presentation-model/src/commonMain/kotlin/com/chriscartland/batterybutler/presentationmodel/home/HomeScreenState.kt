package com.chriscartland.batterybutler.presentationmodel.home

import com.chriscartland.batterybutler.domain.model.Device
import com.chriscartland.batterybutler.domain.model.DeviceImageBytes
import com.chriscartland.batterybutler.domain.model.DeviceType
import com.chriscartland.batterybutler.domain.model.SyncStatus

data class HomeScreenState(
    val groupedDevices: Map<String, List<Device>> = emptyMap(),
    val deviceTypes: Map<String, DeviceType> = emptyMap(),
    val isSortAscending: Boolean = false,
    val isGroupAscending: Boolean = true,
    val sortOption: SortOption = SortOption.BATTERY_AGE,
    val groupOption: GroupOption = GroupOption.NONE,
    val exportData: String? = null,
    val syncStatus: SyncStatus = SyncStatus.Idle,
    val error: String? = null,
    /** Cached photo bytes keyed by [Device.imageEtag], for devices whose photo has finished caching. */
    val deviceImagesByEtag: Map<String, DeviceImageBytes> = emptyMap(),
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
