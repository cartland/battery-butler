package com.chriscartland.batterybutler.usecase

import com.chriscartland.batterybutler.domain.model.AppVersion
import com.chriscartland.batterybutler.domain.repository.AppInfoRepository
import me.tatarka.inject.annotations.Inject

@Inject
class GetAppVersionUseCase(
    private val repository: AppInfoRepository,
) {
    operator fun invoke(): AppVersion = repository.getAppVersion()
}
