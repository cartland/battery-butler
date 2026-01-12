package com.chriscartland.batterybutler

import androidx.compose.ui.window.ComposeUIViewController
import com.chriscartland.batterybutler.data.di.DatabaseFactory
import com.chriscartland.batterybutler.di.IosComponentHelper

@Suppress("ktlint:standard:function-naming")
fun MainViewController() =
    ComposeUIViewController {
        val databaseFactory = DatabaseFactory()
        val component = IosComponentHelper.create(databaseFactory)
        val shareHandler = com.chriscartland.batterybutler.presentation.core.util
            .IosShareHandler()
        App(component, shareHandler)
    }
