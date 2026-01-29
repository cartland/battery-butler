package com.chriscartland.batterybutler.composeapp

import android.content.IntentFilter
import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import androidx.core.content.ContextCompat
import com.chriscartland.batterybutler.BatteryButlerApplication
import com.chriscartland.batterybutler.composeapp.debug.DebugNetworkReceiver
import com.chriscartland.batterybutler.presentationcore.util.AndroidFileSaver
import com.chriscartland.batterybutler.presentationcore.util.AndroidShareHandler

class MainActivity : ComponentActivity() {
    private var debugNetworkReceiver: DebugNetworkReceiver? = null

    override fun onCreate(savedInstanceState: Bundle?) {
        enableEdgeToEdge()
        super.onCreate(savedInstanceState)

        // Reuse application-level component instead of creating duplicate
        val component = (application as BatteryButlerApplication).appComponent
        val shareHandler = AndroidShareHandler(this)
        val fileSaver = AndroidFileSaver(this)

        setContent {
            App(component, shareHandler, fileSaver)
        }

        // DEBUG: Register receiver for ADB control
        // adb shell am broadcast -a com.chriscartland.batterybutler.SET_NETWORK_MODE --es mode "GRPC_LOCAL"
        debugNetworkReceiver = DebugNetworkReceiver(component.setNetworkModeUseCase)
        val filter = IntentFilter(DebugNetworkReceiver.ACTION_SET_NETWORK_MODE)
        ContextCompat.registerReceiver(
            this,
            debugNetworkReceiver,
            filter,
            ContextCompat.RECEIVER_EXPORTED,
        )
    }

    override fun onDestroy() {
        debugNetworkReceiver?.let { unregisterReceiver(it) }
        debugNetworkReceiver = null
        super.onDestroy()
    }
}
