package com.chriscartland.batterybutler.usecase

import com.chriscartland.batterybutler.domain.model.RestoreResult
import com.chriscartland.batterybutler.domain.repository.LegacyDatabaseRepository
import me.tatarka.inject.annotations.Inject

@Inject
class RestoreLegacyDatabaseUseCase(
    private val legacyDatabaseRepository: LegacyDatabaseRepository,
) {
    suspend operator fun invoke(legacyFileName: String): RestoreResult = legacyDatabaseRepository.restoreLegacyDatabase(legacyFileName)
}
