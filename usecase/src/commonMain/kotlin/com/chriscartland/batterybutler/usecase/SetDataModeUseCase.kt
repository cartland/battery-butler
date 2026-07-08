package com.chriscartland.batterybutler.usecase

import com.chriscartland.batterybutler.domain.model.DataMode
import com.chriscartland.batterybutler.domain.repository.DataModeRepository
import me.tatarka.inject.annotations.Inject

@Inject
class SetDataModeUseCase(
    private val repository: DataModeRepository,
) {
    suspend operator fun invoke(mode: DataMode) {
        repository.setDataMode(mode)
    }
}
