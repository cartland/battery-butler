package com.chriscartland.batterybutler.domain.model

sealed interface AppVersion {
    val versionName: String

    data class Android(
        override val versionName: String,
        val versionCode: Long,
    ) : AppVersion

    data class Ios(
        override val versionName: String,
        val buildNumber: String,
    ) : AppVersion

    data class Desktop(
        override val versionName: String,
    ) : AppVersion
}
