package com.chriscartland.batterybutler.composeapp.di

import com.chriscartland.batterybutler.datalocal.room.DatabaseFactory

expect object IosComponentHelper {
    fun create(databaseFactory: DatabaseFactory): AppComponent
}
