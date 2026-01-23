package com.chriscartland.batterybutler.composeapp

import com.chriscartland.batterybutler.composeapp.di.IosComponentHelper
import com.chriscartland.batterybutler.datalocal.di.DatabaseFactory
import com.chriscartland.batterybutler.presentationcore.util.IosFileSaver
import com.chriscartland.batterybutler.presentationcore.util.IosShareHandler

@Suppress("ktlint:standard:function-naming")
fun MainViewController() =
    _root_ide_package_.androidx.compose.ui.window.ComposeUIViewController {
        val databaseFactory = DatabaseFactory()
        val component = IosComponentHelper.create(databaseFactory)
        val shareHandler = IosShareHandler()
        val fileSaver = IosFileSaver()
        App(component, shareHandler, fileSaver)
    }
