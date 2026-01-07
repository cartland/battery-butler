package com.chriscartland.blanket.di

import com.chriscartland.blanket.data.di.DatabaseFactory

actual object IosComponentHelper {
    actual fun create(databaseFactory: DatabaseFactory): AppComponent = AppComponent::class.create(databaseFactory)
}
