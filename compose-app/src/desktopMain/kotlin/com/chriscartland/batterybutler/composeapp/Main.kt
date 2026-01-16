package com.chriscartland.batterybutler.composeapp

import androidx.compose.runtime.CompositionLocalProvider
import androidx.compose.ui.window.Window
import androidx.compose.ui.window.application
import com.chriscartland.batterybutler.composeapp.di.AppComponent
import com.chriscartland.batterybutler.composeapp.feature.ai.NoOpAiEngine
import com.chriscartland.batterybutler.data.di.DatabaseFactory
import com.chriscartland.batterybutler.di.create
import com.chriscartland.batterybutler.domain.repository.RemoteDataSource
import com.chriscartland.batterybutler.domain.repository.RemoteUpdate
import com.chriscartland.batterybutler.presentation.core.util.DesktopFileSaver
import com.chriscartland.batterybutler.presentation.core.util.DesktopShareHandler
import com.chriscartland.batterybutler.presentation.core.util.LocalFileSaver
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.emptyFlow

fun main() =
    application {
        Window(
            onCloseRequest = ::exitApplication,
            title = "Battery Butler",
        ) {
            val databaseFactory = DatabaseFactory()
            val noOpRemoteDataSource = object : RemoteDataSource {
                override fun subscribe(): Flow<RemoteUpdate> = emptyFlow()

                override suspend fun push(update: RemoteUpdate): Boolean = true
            }
            val component =
                AppComponent::class.create(databaseFactory, NoOpAiEngine, noOpRemoteDataSource)
            val shareHandler = DesktopShareHandler()
            val fileSaver = DesktopFileSaver()

            CompositionLocalProvider(
                LocalFileSaver provides fileSaver,
            ) {
                App(component, shareHandler)
            }
        }
    }
