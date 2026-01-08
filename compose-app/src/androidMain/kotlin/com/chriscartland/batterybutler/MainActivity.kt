package com.chriscartland.batterybutler

import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import androidx.compose.runtime.CompositionLocalProvider
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
        
        val networkComponent = com.chriscartland.batterybutler.networking.NetworkComponent(applicationContext)
        val syncService = networkComponent.grpcClient.create(com.chriscartland.batterybutler.proto.SyncServiceClient::class)
        val remoteDataSource = com.chriscartland.batterybutler.networking.GrpcSyncDataSource(syncService)
        
        val component = AppComponent::class.create(databaseFactory, aiEngine, remoteDataSource)
        val shareHandler = com.chriscartland.batterybutler.ui.util
            .AndroidShareHandler(this)
        val fileSaver = com.chriscartland.batterybutler.ui.util
            .AndroidFileSaver(this)

        setContent {
            CompositionLocalProvider(
                com.chriscartland.batterybutler.ui.util.LocalFileSaver provides fileSaver,
            ) {
                App(component, shareHandler)
            }
        }
    }
}
