package com.chriscartland.batterybutler.composeapp.debug

import android.content.BroadcastReceiver
import android.content.Context
import android.content.Intent
import co.touchlab.kermit.Logger
import com.chriscartland.batterybutler.datanetwork.BuildConfig
import com.chriscartland.batterybutler.domain.model.DataMode
import com.chriscartland.batterybutler.usecase.SetDataModeUseCase
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.launch

class DebugDataModeReceiver(
    private val setDataModeUseCase: SetDataModeUseCase,
    private val scope: CoroutineScope,
) : BroadcastReceiver() {
    companion object {
        const val ACTION_SET_DATA_MODE = "com.chriscartland.batterybutler.SET_DATA_MODE"
        const val EXTRA_MODE = "mode"
        private const val TAG = "BatteryButlerReceiver"
    }

    override fun onReceive(
        context: Context?,
        intent: Intent?,
    ) {
        if (intent?.action == ACTION_SET_DATA_MODE) {
            val modeString = intent.getStringExtra(EXTRA_MODE)
            Logger.d(TAG) { "Broadcast received. Mode: $modeString" }

            val mode = when (modeString) {
                // Hardcoded Android Emulator Localhost
                "GRPC_LOCAL" -> DataMode.GrpcLocal("http://10.0.2.2:50051")

                "GRPC_AWS" -> DataMode.GrpcAws(BuildConfig.PRODUCTION_SERVER_URL)

                "GRPC_DEV" -> DataMode.GrpcDev(BuildConfig.DEV_SERVER_URL)

                "MOCK" -> DataMode.Mock

                else -> null
            }

            if (mode != null) {
                // Use injected scope for proper lifecycle management
                scope.launch {
                    setDataModeUseCase(mode)
                    Logger.d(TAG) { "Data mode set to $mode via UseCase" }
                }
            } else {
                Logger.d(TAG) { "Invalid mode received: $modeString" }
            }
        }
    }
}
