package com.chriscartland.batterybutler.data.repository

import com.chriscartland.batterybutler.domain.model.ai.AiEngineType
import com.chriscartland.batterybutler.domain.repository.AiPreferencesRepository
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.asStateFlow

/**
 * In-memory implementation that does not persist preferences across restarts.
 * Used on platforms where persistent AI engine selection is not yet supported.
 */
class InMemoryAiPreferencesRepository(
    defaultType: AiEngineType = AiEngineType.Cloud,
) : AiPreferencesRepository {
    private val _aiEngineType = MutableStateFlow(defaultType)
    override val aiEngineType: Flow<AiEngineType> = _aiEngineType.asStateFlow()

    override suspend fun setAiEngineType(type: AiEngineType) {
        _aiEngineType.value = type
    }
}
