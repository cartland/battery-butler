package com.chriscartland.batterybutler.composeapp

import android.content.IntentFilter
import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import androidx.activity.result.contract.ActivityResultContracts
import androidx.core.content.ContextCompat
import com.chriscartland.batterybutler.BatteryButlerApplication
import com.chriscartland.batterybutler.composeapp.debug.DebugNetworkReceiver
import com.chriscartland.batterybutler.presentationcore.util.AndroidFileLoader
import com.chriscartland.batterybutler.presentationcore.util.AndroidFileSaver
import com.chriscartland.batterybutler.presentationcore.util.AndroidShareHandler

class MainActivity : ComponentActivity() {
    private var debugNetworkReceiver: DebugNetworkReceiver? = null
    private lateinit var fileLoader: AndroidFileLoader

    private val openDocumentLauncher = registerForActivityResult(
        ActivityResultContracts.OpenDocument(),
    ) { uri ->
        fileLoader.handleResult(uri)
    }

    override fun onCreate(savedInstanceState: Bundle?) {
        enableEdgeToEdge()
        super.onCreate(savedInstanceState)

        // Reuse application-level component instead of creating duplicate
        val app = application as BatteryButlerApplication
        val component = app.appComponent

        // Bind activity to GoogleSignInBridge for Credential Manager
        component.googleSignInBridge.bindActivity { this }

        val shareHandler = AndroidShareHandler(this)
        val fileSaver = AndroidFileSaver(this)
        fileLoader = AndroidFileLoader(this, openDocumentLauncher)

        setContent {
            App(component, shareHandler, fileSaver, fileLoader)
        }

        // DEBUG: Register receiver for ADB control
        // adb shell am broadcast -a com.chriscartland.batterybutler.SET_NETWORK_MODE --es mode "GRPC_LOCAL"
        debugNetworkReceiver = DebugNetworkReceiver(component.setNetworkModeUseCase, component.appScope)
        val filter = IntentFilter(DebugNetworkReceiver.ACTION_SET_NETWORK_MODE)
        ContextCompat.registerReceiver(
            this,
            debugNetworkReceiver,
            filter,
            ContextCompat.RECEIVER_EXPORTED,
        )
    }

    override fun onDestroy() {
        // Reuse application-level component
        val app = application as BatteryButlerApplication
        app.appComponent.googleSignInBridge.unbindActivity()

        debugNetworkReceiver?.let { unregisterReceiver(it) }
        debugNetworkReceiver = null
        super.onDestroy()
    }
}
