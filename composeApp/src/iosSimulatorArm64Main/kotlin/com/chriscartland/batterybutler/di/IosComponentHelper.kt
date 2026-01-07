package com.chriscartland.batterybutler.di

import com.chriscartland.batterybutler.data.di.DatabaseFactory

actual object IosComponentHelper {
    actual fun create(databaseFactory: DatabaseFactory): AppComponent = AppComponent::class.create(databaseFactory)
}
