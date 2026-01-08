package com.chriscartland.batterybutler

import androidx.compose.ui.window.Window
import androidx.compose.ui.window.application
import com.chriscartland.batterybutler.data.di.DatabaseFactory
import com.chriscartland.batterybutler.di.AppComponent
import com.chriscartland.batterybutler.di.create
import com.chriscartland.batterybutler.feature.ai.NoOpAiEngine
import com.chriscartland.batterybutler.ui.util.DesktopShareHandler

fun main() =
    application {
        Window(
            onCloseRequest = ::exitApplication,
            title = "Battery Butler",
        ) {
            val databaseFactory = DatabaseFactory()
            val component = AppComponent::class.create(databaseFactory, NoOpAiEngine)
            val shareHandler = DesktopShareHandler()
            val fileSaver = com.chriscartland.batterybutler.ui.util
                .DesktopFileSaver()

            androidx.compose.runtime.CompositionLocalProvider(
                com.chriscartland.batterybutler.ui.util.LocalFileSaver provides fileSaver,
            ) {
                App(component, shareHandler)
            }
        }
    }
