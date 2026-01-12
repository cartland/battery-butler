package com.chriscartland.batterybutler.android

import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import androidx.compose.runtime.CompositionLocalProvider
import com.chriscartland.batterybutler.App
import com.chriscartland.batterybutler.data.ai.AndroidAiEngine
import com.chriscartland.batterybutler.data.di.DatabaseFactory
import com.chriscartland.batterybutler.di.AppComponent
import com.chriscartland.batterybutler.di.create
import com.chriscartland.batterybutler.networking.GrpcSyncDataSource
import com.chriscartland.batterybutler.networking.NetworkComponent
import com.chriscartland.batterybutler.presentation.core.util.AndroidFileSaver
import com.chriscartland.batterybutler.presentation.core.util.AndroidShareHandler
import com.chriscartland.batterybutler.presentation.core.util.LocalFileSaver
import com.chriscartland.batterybutler.proto.SyncServiceClient

class MainActivity : ComponentActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        enableEdgeToEdge()
        super.onCreate(savedInstanceState)

        val databaseFactory = DatabaseFactory(applicationContext)
        val aiEngine = AndroidAiEngine(applicationContext)

        val networkComponent = NetworkComponent(applicationContext)
        val syncService = networkComponent.grpcClient.create(SyncServiceClient::class)
        val remoteDataSource = GrpcSyncDataSource(syncService)

        val component = AppComponent::class.create(databaseFactory, aiEngine, remoteDataSource)
        val shareHandler = AndroidShareHandler(this)
        val fileSaver = AndroidFileSaver(this)

        setContent {
            CompositionLocalProvider(
                LocalFileSaver provides fileSaver,
            ) {
                App(component, shareHandler)
            }
        }
    }
}
