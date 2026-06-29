package com.chriscartland.batterybutler

import android.app.Application
import com.chriscartland.batterybutler.ai.AndroidAiEngine
import com.chriscartland.batterybutler.ai.DynamicAiEngine
import com.chriscartland.batterybutler.ai.OnDeviceAiEngine
import com.chriscartland.batterybutler.composeapp.BuildConfig
import com.chriscartland.batterybutler.composeapp.di.AppComponent
import com.chriscartland.batterybutler.composeapp.di.create
import com.chriscartland.batterybutler.data.provider.DefaultDispatcherProvider
import com.chriscartland.batterybutler.data.repository.AiPreferencesRepositoryImpl
import com.chriscartland.batterybutler.datalocal.preferences.DataStoreFactory
import com.chriscartland.batterybutler.datalocal.room.DatabaseFactory
import com.chriscartland.batterybutler.datanetwork.auth.GoogleSignInBridge
import com.chriscartland.batterybutler.datanetwork.grpc.NetworkComponent
import com.chriscartland.batterybutler.domain.model.AppVersion
import com.russhwolf.settings.SharedPreferencesSettings

class BatteryButlerApplication : Application() {
    val appComponent: AppComponent by lazy {
        val databaseFactory = DatabaseFactory(this)
        val dataStoreFactory = DataStoreFactory(this)
        val networkComponent = NetworkComponent(this)

        // AI Setup
        val settings = SharedPreferencesSettings(getSharedPreferences("ai_prefs", MODE_PRIVATE))
        val aiPreferencesRepository = AiPreferencesRepositoryImpl(settings)
        val cloudAiEngine = AndroidAiEngine()
        val onDeviceAiEngine = OnDeviceAiEngine(this)

        val aiEngine = DynamicAiEngine(
            cloudEngine = cloudAiEngine,
            onDeviceEngine = onDeviceAiEngine,
            aiPreferencesRepository = aiPreferencesRepository,
        )

        val appVersion = AppVersion.Android(
            versionName = BuildConfig.VERSION_NAME,
            versionCode = BuildConfig.VERSION_CODE,
        )
        val googleSignInBridge = GoogleSignInBridge()
        googleSignInBridge.initialize(
            clientId = BuildConfig.GOOGLE_WEB_CLIENT_ID,
            dispatcherProvider = DefaultDispatcherProvider(),
        )
        AppComponent::class.create(
            databaseFactory,
            dataStoreFactory,
            aiEngine,
            networkComponent,
            appVersion,
            aiPreferencesRepository,
            googleSignInBridge,
        )
    }

    override fun onCreate() {
        super.onCreate()
    }
}
