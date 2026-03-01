package com.chriscartland.batterybutler.composeapp.navigation

import androidx.compose.runtime.snapshots.SnapshotStateList
import kotlinx.serialization.Serializable

@Serializable
sealed interface Screen {
    @Serializable
    data object Login : Screen

    @Serializable
    data object Devices : Screen

    @Serializable
    data object History : Screen

    @Serializable
    data object Types : Screen

    @Serializable
    data object Settings : Screen

    @Serializable
    data object AiChat : Screen

    @Serializable
    data object AddDevice : Screen

    @Serializable
    data object AddBatteryEvent : Screen

    @Serializable
    data object AddDeviceType : Screen

    @Serializable
    data class DeviceDetail(
        val deviceId: String,
    ) : Screen

    @Serializable
    data class EditDevice(
        val deviceId: String,
    ) : Screen

    @Serializable
    data class EventDetail(
        val eventId: String,
    ) : Screen

    @Serializable
    data class DeviceTypeDetail(
        val typeId: String,
    ) : Screen

    @Serializable
    data class EditDeviceType(
        val typeId: String,
    ) : Screen

    @Serializable
    data class EditBatteryEvent(
        val eventId: String,
    ) : Screen
}

/**
 * Safely navigates to a screen, preventing duplicate navigation from rapid clicks.
 * If the last screen in the back stack matches the target screen, the navigation is skipped.
 */
fun SnapshotStateList<Screen>.navigateTo(screen: Screen) {
    if (lastOrNull() != screen) {
        add(screen)
    }
}
