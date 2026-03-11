package com.chriscartland.batterybutler.domain.model

sealed interface AppVersion {
    data object Unavailable : AppVersion

    data class Android(
        val versionName: String,
        val versionCode: Int,
    ) : AppVersion

    data class Ios(
        val versionName: String,
        val buildNumber: String,
    ) : AppVersion

    data class Desktop(
        val versionName: String,
    ) : AppVersion
}
