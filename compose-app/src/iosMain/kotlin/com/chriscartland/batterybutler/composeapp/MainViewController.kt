package com.chriscartland.batterybutler.composeapp

import com.chriscartland.batterybutler.composeapp.di.IosComponentHelper
import com.chriscartland.batterybutler.data.di.DatabaseFactory

@Suppress("ktlint:standard:function-naming")
fun MainViewController() =
    _root_ide_package_.androidx.compose.ui.window.ComposeUIViewController {
        val databaseFactory = DatabaseFactory()
        val component = IosComponentHelper.create(databaseFactory)
        val shareHandler = com.chriscartland.batterybutler.presentationcore.util
            .IosShareHandler()
        val fileSaver = com.chriscartland.batterybutler.presentationcore.util
            .IosFileSaver()
        App(component, shareHandler, fileSaver)
    }
