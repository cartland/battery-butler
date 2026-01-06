package com.chriscartland.blanket.di

import com.chriscartland.blanket.data.di.DatabaseFactory

actual object IosComponentHelper {
    actual fun create(databaseFactory: DatabaseFactory): AppComponent {
        return AppComponent::class.create(databaseFactory)
    }
}
