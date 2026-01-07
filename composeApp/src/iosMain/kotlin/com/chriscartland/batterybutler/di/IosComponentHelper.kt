package com.chriscartland.batterybutler.di

import com.chriscartland.batterybutler.data.di.DatabaseFactory

expect object IosComponentHelper {
    fun create(databaseFactory: DatabaseFactory): AppComponent
}
