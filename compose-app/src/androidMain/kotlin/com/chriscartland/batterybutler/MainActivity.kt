package com.chriscartland.batterybutler

import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.compose.runtime.CompositionLocalProvider
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import com.chriscartland.batterybutler.data.ai.AndroidAiEngine
import com.chriscartland.batterybutler.data.di.DatabaseFactory
import com.chriscartland.batterybutler.di.AppComponent
import com.chriscartland.batterybutler.di.create

class MainActivity : ComponentActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        enableEdgeToEdge()
        super.onCreate(savedInstanceState)

        val databaseFactory = DatabaseFactory(applicationContext)
        val aiEngine = AndroidAiEngine(applicationContext)
        val component = AppComponent::class.create(databaseFactory, aiEngine)
        val shareHandler = com.chriscartland.batterybutler.ui.util
            .AndroidShareHandler(this)
        val fileSaver = com.chriscartland.batterybutler.ui.util.AndroidFileSaver(this)

        setContent {
            CompositionLocalProvider(
                com.chriscartland.batterybutler.ui.util.LocalFileSaver provides fileSaver,
            ) {
                App(component, shareHandler)
            }
        }
    }
}
