package com.chriscartland.batterybutler.composeapp.debug

import android.content.BroadcastReceiver
import android.content.Context
import android.content.Intent
import co.touchlab.kermit.Logger
import com.chriscartland.batterybutler.datanetwork.BuildConfig
import com.chriscartland.batterybutler.domain.model.NetworkMode
import com.chriscartland.batterybutler.usecase.SetNetworkModeUseCase
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.launch

class DebugNetworkReceiver(
    private val setNetworkModeUseCase: SetNetworkModeUseCase,
    private val authRepository: com.chriscartland.batterybutler.domain.repository.AuthRepository,
    private val scope: CoroutineScope,
) : BroadcastReceiver() {
    companion object {
        const val ACTION_SET_NETWORK_MODE = "com.chriscartland.batterybutler.SET_NETWORK_MODE"
        const val ACTION_SET_AUTH_TOKEN = "com.chriscartland.batterybutler.SET_AUTH_TOKEN"
        const val EXTRA_MODE = "mode"
        const val EXTRA_URL = "url"
        const val EXTRA_TOKEN = "token"
        private const val TAG = "BatteryButlerReceiver"
    }

    override fun onReceive(
        context: Context?,
        intent: Intent?,
    ) {
        if (intent?.action == ACTION_SET_NETWORK_MODE) {
            val modeString = intent.getStringExtra(EXTRA_MODE)
            val urlString = intent.getStringExtra(EXTRA_URL)
            Logger.d(TAG) { "Broadcast received. Mode: $modeString, URL: $urlString" }

            val mode = when (modeString) {
                "GRPC_LOCAL" -> NetworkMode.GrpcLocal(urlString ?: "http://10.0.2.2:50051")
                "GRPC_AWS" -> NetworkMode.GrpcAws(urlString ?: BuildConfig.PRODUCTION_SERVER_URL)
                "MOCK" -> NetworkMode.Mock
                else -> null
            }

            if (mode != null) {
                // Use injected scope for proper lifecycle management
                scope.launch {
                    setNetworkModeUseCase(mode)
                    Logger.d(TAG) { "Network mode set to $mode via UseCase" }
                }
            } else {
                Logger.d(TAG) { "Invalid mode received: $modeString" }
            }
        } else if (intent?.action == ACTION_SET_AUTH_TOKEN) {
            val token = intent.getStringExtra(EXTRA_TOKEN)
            if (!token.isNullOrBlank()) {
                Logger.d(TAG) { "Broadcast received to set auth token" }
                scope.launch {
                    authRepository.setExternalToken(token)
                    Logger.d(TAG) { "External auth token set" }
                }
            }
        }
    }
}
