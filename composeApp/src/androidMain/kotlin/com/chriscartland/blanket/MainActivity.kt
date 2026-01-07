package com.chriscartland.blanket

import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import com.chriscartland.blanket.data.di.DatabaseFactory
import com.chriscartland.blanket.di.AppComponent
import com.chriscartland.blanket.di.create

class MainActivity : ComponentActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        enableEdgeToEdge()
        super.onCreate(savedInstanceState)

        val databaseFactory = DatabaseFactory(applicationContext)
        val component = AppComponent::class.create(databaseFactory)

        setContent {
            App(component)
        }
    }
}
