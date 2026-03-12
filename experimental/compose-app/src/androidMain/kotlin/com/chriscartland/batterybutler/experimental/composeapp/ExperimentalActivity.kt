package com.chriscartland.batterybutler.experimental.composeapp

import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge

class ExperimentalActivity : ComponentActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        enableEdgeToEdge()
        super.onCreate(savedInstanceState)
        val component = (application as ExperimentalApplication).component
        setContent { ExperimentalApp(component) }
    }
}
