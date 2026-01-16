package com.chriscartland.batterybutler.composeapp.di

import com.chriscartland.batterybutler.composeapp.feature.ai.NoOpAiEngine
import com.chriscartland.batterybutler.data.di.DatabaseFactory
import com.chriscartland.batterybutler.di.create
import com.chriscartland.batterybutler.domain.repository.RemoteDataSource
import com.chriscartland.batterybutler.domain.repository.RemoteUpdate
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.emptyFlow

actual object IosComponentHelper {
    actual fun create(databaseFactory: DatabaseFactory): AppComponent {
        val noOpRemoteDataSource = object : RemoteDataSource {
            override fun subscribe(): Flow<RemoteUpdate> = emptyFlow()

            override suspend fun push(update: RemoteUpdate): Boolean = true
        }
        return AppComponent::class.create(databaseFactory, NoOpAiEngine, noOpRemoteDataSource)
    }
}
