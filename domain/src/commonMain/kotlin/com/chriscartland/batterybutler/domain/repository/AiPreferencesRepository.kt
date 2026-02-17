package com.chriscartland.batterybutler.domain.repository

import com.chriscartland.batterybutler.domain.model.ai.AiEngineType
import kotlinx.coroutines.flow.Flow

interface AiPreferencesRepository {
    val aiEngineType: Flow<AiEngineType>

    suspend fun setAiEngineType(type: AiEngineType)
}
