package com.chriscartland.batterybutler.data.repository

import com.chriscartland.batterybutler.domain.model.AppVersion
import com.chriscartland.batterybutler.domain.repository.AppInfoRepository
import me.tatarka.inject.annotations.Inject

@Inject
class StaticAppInfoRepository(
    private val appVersion: AppVersion,
) : AppInfoRepository {
    override fun getAppVersion(): AppVersion = appVersion
}
