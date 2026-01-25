package com.chriscartland.batterybutler.domain.repository

import com.chriscartland.batterybutler.domain.model.AppVersion

interface AppInfoRepository {
    fun getAppVersion(): AppVersion
}
