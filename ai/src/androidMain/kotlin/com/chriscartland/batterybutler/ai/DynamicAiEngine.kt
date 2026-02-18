package com.chriscartland.batterybutler.ai

import com.chriscartland.batterybutler.domain.model.ai.AiEngine
import com.chriscartland.batterybutler.domain.model.ai.AiEngineType
import com.chriscartland.batterybutler.domain.model.ai.AiMessage
import com.chriscartland.batterybutler.domain.model.ai.AiRole
import com.chriscartland.batterybutler.domain.model.ai.ToolHandler
import com.chriscartland.batterybutler.domain.repository.AiPreferencesRepository
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.flatMapLatest
import kotlinx.coroutines.flow.flowOf
import me.tatarka.inject.annotations.Inject

@Inject
class DynamicAiEngine(
    private val cloudEngine: AndroidAiEngine,
    private val onDeviceEngine: OnDeviceAiEngine,
    private val aiPreferencesRepository: AiPreferencesRepository,
) : AiEngine {
    override val isAvailable: Flow<Boolean> = combine(
        aiPreferencesRepository.aiEngineType,
        cloudEngine.isAvailable,
        onDeviceEngine.isAvailable,
    ) { type, cloud, onDevice ->
        when (type) {
            AiEngineType.Cloud -> cloud
            AiEngineType.OnDevice -> onDevice
            AiEngineType.NoOp -> false
        }
    }

    override val compatibility: Flow<Boolean> = combine(
        aiPreferencesRepository.aiEngineType,
        cloudEngine.compatibility,
        onDeviceEngine.compatibility,
    ) { type, cloud, onDevice ->
        when (type) {
            AiEngineType.Cloud -> cloud
            AiEngineType.OnDevice -> onDevice
            AiEngineType.NoOp -> true
        }
    }

    override suspend fun generateResponse(
        prompt: String,
        toolHandler: ToolHandler?,
    ): Flow<AiMessage> =
        aiPreferencesRepository.aiEngineType.flatMapLatest { type ->
            when (type) {
                AiEngineType.Cloud -> cloudEngine.generateResponse(prompt, toolHandler)
                AiEngineType.OnDevice -> onDeviceEngine.generateResponse(prompt, toolHandler)
                AiEngineType.NoOp -> flowOf(
                    AiMessage(
                        id = "noop",
                        role = AiRole.MODEL,
                        text = "AI is disabled in settings.",
                        isPartial = false,
                    ),
                )
            }
        }
}
