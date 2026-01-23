package com.chriscartland.batterybutler.composeapp.di

import com.chriscartland.batterybutler.datalocal.di.DatabaseFactory

expect object IosComponentHelper {
    fun create(databaseFactory: DatabaseFactory): AppComponent
}
