package com.chriscartland.batterybutler.experimental.composeapp.navigation

import kotlinx.serialization.Serializable

@Serializable
sealed interface ExperimentalScreen {
    @Serializable
    data object Home : ExperimentalScreen

    @Serializable
    data object Counter : ExperimentalScreen
}
