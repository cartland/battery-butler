package com.chriscartland.batterybutler.composeapp.di

import com.chriscartland.batterybutler.ai.NoOpAiEngine
import com.chriscartland.batterybutler.datalocal.preferences.DataStoreFactory
import com.chriscartland.batterybutler.datalocal.room.DatabaseFactory
import com.chriscartland.batterybutler.datanetwork.grpc.NetworkComponent
import com.chriscartland.batterybutler.domain.model.AppVersion

actual object IosComponentHelper {
    actual fun create(
        databaseFactory: DatabaseFactory,
        dataStoreFactory: DataStoreFactory,
        appVersion: AppVersion,
    ): AppComponent {
        val networkComponent = NetworkComponent()
        return AppComponent::class.create(
            databaseFactory,
            dataStoreFactory,
            NoOpAiEngine,
            networkComponent,
            appVersion,
        )
    }
}
