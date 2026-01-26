package com.chriscartland.batterybutler.composeapp

import android.content.IntentFilter
import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import androidx.core.content.ContextCompat
import com.chriscartland.batterybutler.ai.AndroidAiEngine
import com.chriscartland.batterybutler.composeapp.debug.DebugNetworkReceiver
import com.chriscartland.batterybutler.composeapp.di.AppComponent
import com.chriscartland.batterybutler.composeapp.di.create
import com.chriscartland.batterybutler.datalocal.room.DatabaseFactory
import com.chriscartland.batterybutler.datanetwork.grpc.NetworkComponent
import com.chriscartland.batterybutler.domain.model.AppVersion
import com.chriscartland.batterybutler.presentationcore.util.AndroidFileSaver
import com.chriscartland.batterybutler.presentationcore.util.AndroidShareHandler

class MainActivity : ComponentActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        enableEdgeToEdge()
        super.onCreate(savedInstanceState)

        val databaseFactory = DatabaseFactory(applicationContext)
        val aiEngine = AndroidAiEngine()

        val networkComponent = NetworkComponent(applicationContext)

        val appVersion = AppVersion.Android(
            versionName = BuildConfig.VERSION_NAME,
            versionCode = BuildConfig.VERSION_CODE,
        )

        val component = AppComponent::class.create(databaseFactory, aiEngine, networkComponent, appVersion)
        val shareHandler = AndroidShareHandler(this)
        val fileSaver = AndroidFileSaver(this)

        setContent {
            App(component, shareHandler, fileSaver)
        }

        // DEBUG: Register receiver for ADB control
        // adb shell am broadcast -a com.chriscartland.batterybutler.SET_NETWORK_MODE --es mode "GRPC_LOCAL"
        val receiver = DebugNetworkReceiver(component.setNetworkModeUseCase)
        val filter = IntentFilter(DebugNetworkReceiver.ACTION_SET_NETWORK_MODE)
        ContextCompat.registerReceiver(
            this,
            receiver,
            filter,
            ContextCompat.RECEIVER_EXPORTED,
        )
    }
}
