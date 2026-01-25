package com.chriscartland.batterybutler.composeapp

import androidx.compose.ui.window.Window
import androidx.compose.ui.window.application
import com.chriscartland.batterybutler.composeapp.di.AppComponent
import com.chriscartland.batterybutler.composeapp.di.create
import com.chriscartland.batterybutler.composeapp.feature.ai.NoOpAiEngine
import com.chriscartland.batterybutler.datalocal.room.DatabaseFactory
import com.chriscartland.batterybutler.datanetwork.grpc.NetworkComponent
import com.chriscartland.batterybutler.presentationcore.util.DesktopFileSaver
import com.chriscartland.batterybutler.presentationcore.util.DesktopShareHandler

import com.chriscartland.batterybutler.domain.model.AppVersion

fun main() =
    application {
        Window(
            onCloseRequest = ::exitApplication,
            title = "Battery Butler",
        ) {
            val databaseFactory = DatabaseFactory()
            val networkComponent = NetworkComponent()
            val appVersion = AppVersion(
                versionName = "1.0.0",
                versionCode = 1,
            )
            val component =
                AppComponent::class.create(databaseFactory, NoOpAiEngine, networkComponent, appVersion)
            val shareHandler = DesktopShareHandler()
            val fileSaver = DesktopFileSaver()

            App(component, shareHandler, fileSaver)
        }
    }
