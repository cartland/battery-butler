package com.chriscartland.batterybutler.domain.model

sealed interface NetworkMode {
    data object Mock : NetworkMode

    data class GrpcLocal(
        val url: String?,
    ) : NetworkMode

    data class GrpcAws(
        val url: String?,
    ) : NetworkMode
}
