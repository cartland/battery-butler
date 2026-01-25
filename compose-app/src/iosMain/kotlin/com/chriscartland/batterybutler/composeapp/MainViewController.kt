package com.chriscartland.batterybutler.composeapp

import com.chriscartland.batterybutler.composeapp.di.IosComponentHelper
import com.chriscartland.batterybutler.datalocal.room.DatabaseFactory
import com.chriscartland.batterybutler.presentationcore.util.IosFileSaver
import com.chriscartland.batterybutler.presentationcore.util.IosShareHandler

@Suppress("ktlint:standard:function-naming")
fun MainViewController() =
    _root_ide_package_.androidx.compose.ui.window.ComposeUIViewController {
        val databaseFactory = DatabaseFactory()
        val component = IosComponentHelper.create(databaseFactory)
        val shareHandler = IosShareHandler()
        val fileSaver = IosFileSaver()
        val shareHandler = IosShareHandler()
        val fileSaver = IosFileSaver()
        val bundle = platform.Foundation.NSBundle.mainBundle
        val version = bundle.infoDictionary?.get("CFBundleShortVersionString") as? String ?: "Unknown"
        val build = bundle.infoDictionary?.get("CFBundleVersion") as? String ?: "0"
        val appVersion = "$version-$build"
        App(component, shareHandler, fileSaver, appVersion)
    }
