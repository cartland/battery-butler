package com.chriscartland.batterybutler.composeapp.di

import com.chriscartland.batterybutler.composeapp.di.AppComponent
import com.chriscartland.batterybutler.data.di.DatabaseFactory

expect object IosComponentHelper {
    fun create(databaseFactory: DatabaseFactory): AppComponent
}
