package com.chriscartland.batterybutler.composeapp

import androidx.compose.ui.window.Window
import androidx.compose.ui.window.application
import com.chriscartland.batterybutler.composeapp.di.AppComponent
import com.chriscartland.batterybutler.composeapp.di.create
import com.chriscartland.batterybutler.composeapp.feature.ai.NoOpAiEngine
import com.chriscartland.batterybutler.datalocal.di.DatabaseFactory
import com.chriscartland.batterybutler.datanetwork.grpc.NetworkComponent
import com.chriscartland.batterybutler.presentationcore.util.DesktopFileSaver
import com.chriscartland.batterybutler.presentationcore.util.DesktopShareHandler

fun main() =
    application {
        Window(
            onCloseRequest = ::exitApplication,
            title = "Battery Butler",
        ) {
            val databaseFactory = DatabaseFactory()
            val networkComponent = NetworkComponent()
            val component =
                AppComponent::class.create(databaseFactory, NoOpAiEngine, networkComponent)
            val shareHandler = DesktopShareHandler()
            val fileSaver = DesktopFileSaver()

            App(component, shareHandler, fileSaver)
        }
    }
