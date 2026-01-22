package com.chriscartland.batterybutler.composeapp

import android.content.BroadcastReceiver
import android.content.Context
import android.content.Intent
import android.util.Log
import com.chriscartland.batterybutler.networking.SharedServerConfig

class ServerUrlReceiver : BroadcastReceiver() {
    override fun onReceive(
        context: Context,
        intent: Intent,
    ) {
        if (intent.action == "com.chriscartland.batterybutler.SET_SERVER_URL") {
            val url = intent.getStringExtra("url")
            if (url != null) {
                Log.d("ServerUrlReceiver", "Updating Server URL to: $url")
                SharedServerConfig.setServerUrl(url)
            } else {
                Log.d("ServerUrlReceiver", "Updating Server URL: Resetting to default")
                SharedServerConfig.resetServerUrl()
            }
        }
    }
}
