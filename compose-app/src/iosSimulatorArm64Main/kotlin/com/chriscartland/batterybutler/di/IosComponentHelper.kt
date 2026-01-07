package com.chriscartland.batterybutler.di

import com.chriscartland.batterybutler.data.di.DatabaseFactory
import com.chriscartland.batterybutler.feature.ai.NoOpAiEngine

actual object IosComponentHelper {
    actual fun create(databaseFactory: DatabaseFactory): AppComponent = AppComponent::class.create(databaseFactory, NoOpAiEngine)
}
