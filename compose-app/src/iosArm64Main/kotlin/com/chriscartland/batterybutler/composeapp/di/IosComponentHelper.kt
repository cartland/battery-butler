package com.chriscartland.batterybutler.composeapp.di

import com.chriscartland.batterybutler.ai.NoOpAiEngine
import com.chriscartland.batterybutler.datalocal.preferences.DataStoreFactory
import com.chriscartland.batterybutler.datalocal.room.DatabaseFactory
import com.chriscartland.batterybutler.datanetwork.auth.GoogleSignInBridge
import com.chriscartland.batterybutler.datanetwork.grpc.NetworkComponent
import com.chriscartland.batterybutler.domain.model.AppVersion

actual object IosComponentHelper {
    actual fun create(
        databaseFactory: DatabaseFactory,
        dataStoreFactory: DataStoreFactory,
        appVersion: AppVersion,
    ): AppComponent {
        val networkComponent = NetworkComponent()
        val googleSignInBridge = GoogleSignInBridge()
        // iOS client ID comes from Info.plist via Swift interop (future work)
        // For now, initialize without ID - will show "Coming Soon"
        googleSignInBridge.initialize(null)
        return AppComponent::class.create(
            databaseFactory,
            dataStoreFactory,
            NoOpAiEngine,
            networkComponent,
            appVersion,
            googleSignInBridge,
        )
    }
}
