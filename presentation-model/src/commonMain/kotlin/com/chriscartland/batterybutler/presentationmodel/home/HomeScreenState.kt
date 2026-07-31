package com.chriscartland.batterybutler.presentationmodel.home

import com.chriscartland.batterybutler.domain.model.Device
import com.chriscartland.batterybutler.domain.model.DeviceImageBytes
import com.chriscartland.batterybutler.domain.model.DeviceType
import com.chriscartland.batterybutler.domain.model.DisplayDensity
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
    val densityOption: DensityOption = DensityOption.EXPANDED,
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

/**
 * How much vertical room each device row occupies in the list.
 *
 * [EXPANDED] is the original two-line row: name plus a "type • location" secondary line, with a
 * 48.dp icon/photo. [COMPACT] drops the secondary line and shrinks the icon so the card is only as
 * tall as its single line of text — name on the left, battery age on the right.
 */
enum class DensityOption {
    EXPANDED,
    COMPACT,
}

/**
 * The rendered form of the stored [DisplayDensity].
 *
 * [DensityOption] deliberately has no `UNSPECIFIED`: by the time a screen state is built the
 * question "has the user chosen?" is already answered, and a UI enum with an un-renderable third
 * case would force every consumer — including the Swift `HomeScreenState` constructions in
 * `iosAppSwiftUITests` — to handle a case that never reaches them.
 */
fun DisplayDensity.toDensityOption(): DensityOption = if (isCompact) DensityOption.COMPACT else DensityOption.EXPANDED

/** The stored form of a user's explicit pick. Never produces [DisplayDensity.UNSPECIFIED]. */
fun DensityOption.toDisplayDensity(): DisplayDensity =
    when (this) {
        DensityOption.EXPANDED -> DisplayDensity.EXPANDED
        DensityOption.COMPACT -> DisplayDensity.COMPACT
    }
