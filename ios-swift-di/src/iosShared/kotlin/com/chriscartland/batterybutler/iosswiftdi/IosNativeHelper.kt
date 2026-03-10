package com.chriscartland.batterybutler.iosswiftdi

import com.chriscartland.batterybutler.ai.NoOpAiEngine
import com.chriscartland.batterybutler.datalocal.preferences.DataStoreFactory
import com.chriscartland.batterybutler.datalocal.room.DatabaseFactory
import com.chriscartland.batterybutler.datanetwork.NoOpRemoteDataSource
import com.chriscartland.batterybutler.domain.repository.NoOpAuthRepository

class IosNativeHelper {
    fun createComponent(): NativeComponent {
        val databaseFactory = DatabaseFactory()
        val dataStoreFactory = DataStoreFactory()
        val component = InjectNativeComponent(
            databaseFactory,
            dataStoreFactory,
            NoOpAiEngine,
            NoOpRemoteDataSource,
            NoOpAuthRepository,
        )
        return component
    }
}
