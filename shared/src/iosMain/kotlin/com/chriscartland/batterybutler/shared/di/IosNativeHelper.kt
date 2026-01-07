package com.chriscartland.batterybutler.shared.di

import com.chriscartland.batterybutler.data.di.DatabaseFactory
import com.chriscartland.batterybutler.domain.ai.AiEngine
import com.chriscartland.batterybutler.domain.ai.AiMessage
import com.chriscartland.batterybutler.domain.ai.ToolHandler
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.flowOf

class IosNativeHelper {
    fun createComponent(): NativeComponent {
        val databaseFactory = DatabaseFactory()
        val noOpAiEngine = object : AiEngine {
            override val isAvailable: Flow<Boolean> = flowOf(false)
            override suspend fun generateResponse(prompt: String, toolHandler: ToolHandler?): Flow<AiMessage> = flowOf()
            override val compatibility: Flow<Boolean> = flowOf(false)
        }
        // val component = InjectNativeComponent(databaseFactory, noOpAiEngine)
        // return component
        TODO("Run KSP to generate InjectNativeComponent")
    }
}
