package com.chriscartland.batterybutler

import android.app.Application
import com.chriscartland.batterybutler.ai.AndroidAiEngine
import com.chriscartland.batterybutler.composeapp.BuildConfig
import com.chriscartland.batterybutler.composeapp.di.AppComponent
import com.chriscartland.batterybutler.composeapp.di.create
import com.chriscartland.batterybutler.config.BuildConfigAiConfig
import com.chriscartland.batterybutler.datalocal.preferences.DataStoreFactory
import com.chriscartland.batterybutler.datalocal.room.DatabaseFactory
import com.chriscartland.batterybutler.datanetwork.grpc.NetworkComponent
import com.chriscartland.batterybutler.domain.model.AppVersion

class BatteryButlerApplication : Application() {
    lateinit var appComponent: AppComponent
        private set

    override fun onCreate() {
        super.onCreate()
        val databaseFactory = DatabaseFactory(this)
        val dataStoreFactory = DataStoreFactory(this)
        val networkComponent = NetworkComponent(this)
        val aiConfig = BuildConfigAiConfig()
        val aiEngine = AndroidAiEngine(aiConfig)
        val appVersion = AppVersion.Android(
            versionName = BuildConfig.VERSION_NAME,
            versionCode = BuildConfig.VERSION_CODE,
        )
        appComponent = AppComponent::class.create(
            databaseFactory,
            dataStoreFactory,
            aiEngine,
            networkComponent,
            appVersion,
        )
    }
}
