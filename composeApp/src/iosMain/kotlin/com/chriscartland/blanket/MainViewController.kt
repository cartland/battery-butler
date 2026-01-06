package com.chriscartland.blanket

import androidx.compose.ui.window.ComposeUIViewController
import com.chriscartland.blanket.data.di.DatabaseFactory
import com.chriscartland.blanket.di.AppComponent
import com.chriscartland.blanket.di.IosComponentHelper

@Suppress("ktlint:standard:function-naming")
fun MainViewController() =
    ComposeUIViewController {
        val databaseFactory = DatabaseFactory()
        val component = IosComponentHelper.create(databaseFactory)
        App(component)
    }
