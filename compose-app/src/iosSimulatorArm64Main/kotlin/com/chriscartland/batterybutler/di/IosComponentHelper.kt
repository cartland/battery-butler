package com.chriscartland.batterybutler.di

import com.chriscartland.batterybutler.data.di.DatabaseFactory
import com.chriscartland.batterybutler.feature.ai.NoOpAiEngine

actual object IosComponentHelper {
    actual fun create(databaseFactory: DatabaseFactory): AppComponent {
        val noOpRemoteDataSource = object : com.chriscartland.batterybutler.domain.repository.RemoteDataSource {
            override fun subscribe(): kotlinx.coroutines.flow.Flow<com.chriscartland.batterybutler.domain.repository.RemoteUpdate> = kotlinx.coroutines.flow.emptyFlow()
            override suspend fun push(update: com.chriscartland.batterybutler.domain.repository.RemoteUpdate): Boolean = true
        }
        return AppComponent::class.create(databaseFactory, NoOpAiEngine, noOpRemoteDataSource)
    }
}
