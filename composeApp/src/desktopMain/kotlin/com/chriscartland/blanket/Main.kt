package com.chriscartland.blanket

import androidx.compose.ui.window.Window
import androidx.compose.ui.window.application
import com.chriscartland.blanket.data.di.DatabaseFactory
import com.chriscartland.blanket.di.AppComponent
import com.chriscartland.blanket.di.create

fun main() =
    application {
        Window(
            onCloseRequest = ::exitApplication,
            title = "Blanket",
        ) {
            val databaseFactory = DatabaseFactory()
            val component = AppComponent::class.create(databaseFactory)
            App(component)
        }
    }
