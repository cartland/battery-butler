package com.chriscartland.batterybutler

import android.app.Application
import com.chriscartland.batterybutler.ai.AndroidAiEngine
import com.chriscartland.batterybutler.composeapp.di.AppComponent
import com.chriscartland.batterybutler.composeapp.di.create
import com.chriscartland.batterybutler.datalocal.room.DatabaseFactory
import com.chriscartland.batterybutler.datanetwork.grpc.NetworkComponent

import com.chriscartland.batterybutler.composeapp.BuildConfig
import com.chriscartland.batterybutler.domain.model.AppVersion

class BatteryButlerApplication : Application() {
    lateinit var appComponent: AppComponent
        private set

    override fun onCreate() {
        super.onCreate()
        val databaseFactory = DatabaseFactory(this)
        val networkComponent = NetworkComponent(this)
        val aiEngine = AndroidAiEngine()
        val appVersion = AppVersion.Android(
            versionName = BuildConfig.VERSION_NAME,
            versionCode = BuildConfig.VERSION_CODE.toLong(),
        )
        appComponent = AppComponent::class.create(databaseFactory, aiEngine, networkComponent, appVersion)
    }
}
