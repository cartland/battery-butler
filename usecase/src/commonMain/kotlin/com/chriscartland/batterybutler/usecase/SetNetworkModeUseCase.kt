package com.chriscartland.batterybutler.usecase

import com.chriscartland.batterybutler.domain.model.NetworkMode
import com.chriscartland.batterybutler.domain.repository.NetworkModeRepository
import me.tatarka.inject.annotations.Inject

@Inject
class SetNetworkModeUseCase(
    private val repository: NetworkModeRepository,
) {
    suspend operator fun invoke(mode: NetworkMode) {
        repository.setNetworkMode(mode)
    }
}
