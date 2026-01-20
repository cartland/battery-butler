package com.chriscartland.batterybutler.composeapp.di

import com.chriscartland.batterybutler.composeapp.di.create
import com.chriscartland.batterybutler.composeapp.feature.ai.NoOpAiEngine
import com.chriscartland.batterybutler.data.di.DatabaseFactory
import com.chriscartland.batterybutler.networking.NetworkComponent

actual object IosComponentHelper {
    actual fun create(databaseFactory: DatabaseFactory): AppComponent {
        val networkComponent = NetworkComponent()
        return AppComponent::class.create(databaseFactory, NoOpAiEngine, networkComponent.grpcClient)
    }
}
