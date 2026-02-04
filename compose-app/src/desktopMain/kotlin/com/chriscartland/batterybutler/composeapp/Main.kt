package com.chriscartland.batterybutler.composeapp

import androidx.compose.ui.window.Window
import androidx.compose.ui.window.application
import com.chriscartland.batterybutler.ai.NoOpAiEngine
import com.chriscartland.batterybutler.composeapp.di.AppComponent
import com.chriscartland.batterybutler.composeapp.di.create
import com.chriscartland.batterybutler.datalocal.preferences.DataStoreFactory
import com.chriscartland.batterybutler.datalocal.room.DatabaseFactory
import com.chriscartland.batterybutler.datanetwork.grpc.NetworkComponent
import com.chriscartland.batterybutler.domain.model.AppVersion
import com.chriscartland.batterybutler.presentationcore.util.DesktopFileSaver
import com.chriscartland.batterybutler.presentationcore.util.DesktopShareHandler

fun main() =
    application {
        Window(
            onCloseRequest = ::exitApplication,
            title = "Battery Butler",
        ) {
            val databaseFactory = DatabaseFactory()
            val dataStoreFactory = DataStoreFactory()
            val networkComponent = NetworkComponent()
            val appVersion = AppVersion.Desktop(
                versionName = "1.0.0",
            )
            val component =
                AppComponent::class.create(
                    databaseFactory,
                    dataStoreFactory,
                    NoOpAiEngine,
                    networkComponent,
                    appVersion,
                )
            val shareHandler = DesktopShareHandler()
            val fileSaver = DesktopFileSaver()

            App(component, shareHandler, fileSaver)
        }
    }
