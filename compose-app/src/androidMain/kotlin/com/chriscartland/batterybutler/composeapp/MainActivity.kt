package com.chriscartland.batterybutler.composeapp

import android.content.IntentFilter
import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import androidx.activity.result.contract.ActivityResultContracts
import androidx.core.content.ContextCompat
import androidx.core.splashscreen.SplashScreen.Companion.installSplashScreen
import com.chriscartland.batterybutler.BatteryButlerApplication
import com.chriscartland.batterybutler.composeapp.debug.DebugDataModeReceiver
import com.chriscartland.batterybutler.presentationcore.util.AndroidAppRestarter
import com.chriscartland.batterybutler.presentationcore.util.AndroidDeviceImagePicker
import com.chriscartland.batterybutler.presentationcore.util.AndroidFileLoader
import com.chriscartland.batterybutler.presentationcore.util.AndroidFileSaver
import com.chriscartland.batterybutler.presentationcore.util.AndroidSecureClipboard
import com.chriscartland.batterybutler.presentationcore.util.AndroidShareHandler

class MainActivity : ComponentActivity() {
    private var debugDataModeReceiver: DebugDataModeReceiver? = null
    private lateinit var fileLoader: AndroidFileLoader
    private lateinit var deviceImagePicker: AndroidDeviceImagePicker

    private val openDocumentLauncher = registerForActivityResult(
        ActivityResultContracts.OpenDocument(),
    ) { uri ->
        fileLoader.handleResult(uri)
    }

    private val pickDeviceImageLauncher = registerForActivityResult(
        ActivityResultContracts.PickVisualMedia(),
    ) { uri ->
        deviceImagePicker.handleResult(uri)
    }

    override fun onCreate(savedInstanceState: Bundle?) {
        installSplashScreen()
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
        val secureClipboard = AndroidSecureClipboard(this)
        val appRestarter = AndroidAppRestarter(this)
        deviceImagePicker = AndroidDeviceImagePicker(this, pickDeviceImageLauncher)

        setContent {
            App(component, shareHandler, fileSaver, fileLoader, secureClipboard, appRestarter, deviceImagePicker)
        }

        // DEBUG: Register receiver for ADB control
        // adb shell am broadcast -a com.chriscartland.batterybutler.SET_DATA_MODE --es mode "GRPC_LOCAL"
        debugDataModeReceiver = DebugDataModeReceiver(component.setDataModeUseCase, component.appScope)
        val filter = IntentFilter(DebugDataModeReceiver.ACTION_SET_DATA_MODE)
        ContextCompat.registerReceiver(
            this,
            debugDataModeReceiver,
            filter,
            ContextCompat.RECEIVER_EXPORTED,
        )
    }

    override fun onDestroy() {
        // Reuse application-level component
        val app = application as BatteryButlerApplication
        app.appComponent.googleSignInBridge.unbindActivity()

        debugDataModeReceiver?.let { unregisterReceiver(it) }
        debugDataModeReceiver = null
        super.onDestroy()
    }
}
