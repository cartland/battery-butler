package com.chriscartland.batterybutler.data.repository

import com.chriscartland.batterybutler.domain.model.ai.AiEngineType
import com.chriscartland.batterybutler.domain.repository.AiPreferencesRepository
import com.russhwolf.settings.Settings
import com.russhwolf.settings.set
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.asStateFlow
import me.tatarka.inject.annotations.Inject

@Inject
class AiPreferencesRepositoryImpl(
    private val settings: Settings,
) : AiPreferencesRepository {
    private val _aiEngineType = MutableStateFlow(
        AiEngineType.entries.find { it.name == settings.getString(KEY_AI_ENGINE_TYPE, AiEngineType.Cloud.name) }
            ?: AiEngineType.Cloud
    )
    override val aiEngineType: Flow<AiEngineType> = _aiEngineType.asStateFlow()

    override suspend fun setAiEngineType(type: AiEngineType) {
        settings[KEY_AI_ENGINE_TYPE] = type.name
        _aiEngineType.value = type
    }

    companion object {
        private const val KEY_AI_ENGINE_TYPE = "ai_engine_type"
    }
}
