package com.chriscartland.batterybutler.composeapp.debug

import android.content.BroadcastReceiver
import android.content.Context
import android.content.Intent
import com.chriscartland.batterybutler.domain.AppLogger
import com.chriscartland.batterybutler.domain.model.NetworkMode
import com.chriscartland.batterybutler.usecase.SetNetworkModeUseCase
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch

class DebugNetworkReceiver(
    private val setNetworkModeUseCase: SetNetworkModeUseCase,
) : BroadcastReceiver() {
    companion object {
        const val ACTION_SET_NETWORK_MODE = "com.chriscartland.batterybutler.SET_NETWORK_MODE"
        const val EXTRA_MODE = "mode"
        private const val TAG = "BatteryButlerReceiver"
    }

    override fun onReceive(
        context: Context?,
        intent: Intent?,
    ) {
        if (intent?.action == ACTION_SET_NETWORK_MODE) {
            val modeString = intent.getStringExtra(EXTRA_MODE)
            AppLogger.d(TAG, "Broadcast received. Mode: $modeString")

            val mode = when (modeString) {
                "GRPC_LOCAL" -> NetworkMode.GRPC_LOCAL
                "GRPC_AWS" -> NetworkMode.GRPC_AWS
                "MOCK" -> NetworkMode.MOCK
                else -> null
            }

            if (mode != null) {
                // Launch on a detached scope since Receiver context is short-lived
                CoroutineScope(Dispatchers.Default).launch {
                    setNetworkModeUseCase(mode)
                    AppLogger.d(TAG, "Network mode set to $mode via UseCase")
                }
            } else {
                AppLogger.d(TAG, "Invalid mode received: $modeString")
            }
        }
    }
}
