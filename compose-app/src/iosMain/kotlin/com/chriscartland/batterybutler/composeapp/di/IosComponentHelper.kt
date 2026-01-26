package com.chriscartland.batterybutler.composeapp.di

import com.chriscartland.batterybutler.datalocal.room.DatabaseFactory
import com.chriscartland.batterybutler.domain.model.AppVersion

expect object IosComponentHelper {
    fun create(
        databaseFactory: DatabaseFactory,
        appVersion: AppVersion,
    ): AppComponent
}
