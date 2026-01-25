package com.chriscartland.batterybutler

import android.app.Application
import com.chriscartland.batterybutler.ai.AndroidAiEngine
import com.chriscartland.batterybutler.composeapp.di.AppComponent
import com.chriscartland.batterybutler.composeapp.di.create
import com.chriscartland.batterybutler.datalocal.room.DatabaseFactory
import com.chriscartland.batterybutler.datanetwork.grpc.NetworkComponent

class BatteryButlerApplication : Application() {
    lateinit var appComponent: AppComponent
        private set

    override fun onCreate() {
        super.onCreate()
        val databaseFactory = DatabaseFactory(this)
        val networkComponent = NetworkComponent(this)
        val aiEngine = AndroidAiEngine()
        appComponent = AppComponent::class.create(databaseFactory, aiEngine, networkComponent)
    }
}
