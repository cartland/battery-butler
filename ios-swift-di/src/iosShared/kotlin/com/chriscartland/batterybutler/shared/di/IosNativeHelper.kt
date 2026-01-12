package com.chriscartland.batterybutler.shared.di

import com.chriscartland.batterybutler.data.di.DatabaseFactory
import com.chriscartland.batterybutler.domain.ai.AiEngine
import com.chriscartland.batterybutler.domain.ai.AiMessage
import com.chriscartland.batterybutler.domain.ai.ToolHandler
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.flowOf

import com.chriscartland.batterybutler.domain.repository.RemoteDataSource
import com.chriscartland.batterybutler.domain.repository.RemoteUpdate

class IosNativeHelper {
    fun createComponent(): NativeComponent {
        val databaseFactory = DatabaseFactory()
        val noOpAiEngine = object : AiEngine {
            override val isAvailable: Flow<Boolean> = flowOf(false)

            override suspend fun generateResponse(
                prompt: String,
                toolHandler: ToolHandler?,
            ): Flow<AiMessage> = flowOf()

            override val compatibility: Flow<Boolean> = flowOf(false)
        }
        val noOpRemoteDataSource = object : RemoteDataSource {
            override fun subscribe(): Flow<RemoteUpdate> = flowOf()

            override suspend fun push(update: RemoteUpdate): Boolean = true
        }
        val component = InjectNativeComponent(databaseFactory, noOpAiEngine, noOpRemoteDataSource)
        return component
    }
}
