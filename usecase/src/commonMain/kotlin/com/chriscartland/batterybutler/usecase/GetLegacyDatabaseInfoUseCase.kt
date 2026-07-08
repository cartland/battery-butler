package com.chriscartland.batterybutler.usecase

import com.chriscartland.batterybutler.domain.model.DataMode
import com.chriscartland.batterybutler.domain.model.LegacyDatabaseInfo
import com.chriscartland.batterybutler.domain.repository.LegacyDatabaseRepository
import me.tatarka.inject.annotations.Inject

@Inject
class GetLegacyDatabaseInfoUseCase(
    private val legacyDatabaseRepository: LegacyDatabaseRepository,
) {
    operator fun invoke(dataMode: DataMode): LegacyDatabaseInfo? = legacyDatabaseRepository.getLegacyDatabaseInfo(dataMode)
}
