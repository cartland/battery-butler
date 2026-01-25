package com.chriscartland.batterybutler.composeapp.di

import com.chriscartland.batterybutler.composeapp.di.create
import com.chriscartland.batterybutler.composeapp.feature.ai.NoOpAiEngine
import com.chriscartland.batterybutler.datalocal.room.DatabaseFactory
import com.chriscartland.batterybutler.datanetwork.grpc.NetworkComponent

import com.chriscartland.batterybutler.domain.model.AppVersion

actual object IosComponentHelper {
    actual fun create(databaseFactory: DatabaseFactory, appVersion: AppVersion): AppComponent {
        val networkComponent = NetworkComponent()
        return AppComponent::class.create(databaseFactory, NoOpAiEngine, networkComponent, appVersion)
    }
}
