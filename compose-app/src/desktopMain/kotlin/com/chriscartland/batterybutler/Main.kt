package com.chriscartland.batterybutler

import androidx.compose.ui.window.Window
import androidx.compose.ui.window.application
import com.chriscartland.batterybutler.data.di.DatabaseFactory
import com.chriscartland.batterybutler.di.AppComponent
import com.chriscartland.batterybutler.di.create
import com.chriscartland.batterybutler.feature.ai.NoOpAiEngine
import com.chriscartland.batterybutler.presentation.core.util.DesktopShareHandler

fun main() =
    application {
        Window(
            onCloseRequest = ::exitApplication,
            title = "Battery Butler",
        ) {
            val databaseFactory = DatabaseFactory()
            val noOpRemoteDataSource = object : com.chriscartland.batterybutler.domain.repository.RemoteDataSource {
                override fun subscribe(): kotlinx.coroutines.flow.Flow<com.chriscartland.batterybutler.domain.repository.RemoteUpdate> =
                    kotlinx.coroutines.flow.emptyFlow()

                override suspend fun push(update: com.chriscartland.batterybutler.domain.repository.RemoteUpdate): Boolean = true
            }
            val component = AppComponent::class.create(databaseFactory, NoOpAiEngine, noOpRemoteDataSource)
            val shareHandler = DesktopShareHandler()
            val fileSaver = com.chriscartland.batterybutler.presentation.core.util
                .DesktopFileSaver()

            androidx.compose.runtime.CompositionLocalProvider(
                com.chriscartland.batterybutler.presentation.core.util.LocalFileSaver provides fileSaver,
            ) {
                App(component, shareHandler)
            }
        }
    }
