package com.chriscartland.batterybutler

import androidx.compose.ui.window.Window
import androidx.compose.ui.window.application
import com.chriscartland.batterybutler.data.di.DatabaseFactory
import com.chriscartland.batterybutler.di.AppComponent
import com.chriscartland.batterybutler.di.create

fun main() =
    application {
        Window(
            onCloseRequest = ::exitApplication,
            title = "Battery Butler",
        ) {
            val databaseFactory = DatabaseFactory()
            val component = AppComponent::class.create(databaseFactory)
            App(component)
        }
    }
