package com.chriscartland.batterybutler.testcommon

import com.chriscartland.batterybutler.domain.model.ai.AiEngineType
import com.chriscartland.batterybutler.domain.repository.AiPreferencesRepository
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.MutableStateFlow

/**
 * Fake implementation of [AiPreferencesRepository] for testing.
 *
 * Backed by a [MutableStateFlow] for reactive updates.
 *
 * Example usage:
 * ```kotlin
 * val repo = FakeAiPreferencesRepository()
 * repo.setAiEngineType(AiEngineType.Cloud)
 * assertEquals(AiEngineType.Cloud, repo.aiEngineType.first())
 * ```
 */
class FakeAiPreferencesRepository(
    initialType: AiEngineType = AiEngineType.NoOp,
) : AiPreferencesRepository {
    private val _aiEngineType = MutableStateFlow(initialType)

    override val aiEngineType: Flow<AiEngineType> = _aiEngineType

    override suspend fun setAiEngineType(type: AiEngineType) {
        _aiEngineType.value = type
    }
}
