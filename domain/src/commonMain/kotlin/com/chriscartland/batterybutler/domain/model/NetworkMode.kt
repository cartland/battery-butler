package com.chriscartland.batterybutler.domain.model

/**
 * Represents the network connectivity mode for the application.
 *
 * @property Mock Runs completely offline with simulated data.
 * @property GrpcLocal Connects to a locally running gRPC server (e.g., localhost).
 * @property GrpcAws Connects to the production AWS gRPC environment.
 */
sealed interface NetworkMode {
    /**
     * Offline mock mode using static fixtures.
     */
    data object Mock : NetworkMode

    /**
     * Local development server mode.
     * @param url Optional override URL.
     */
    data class GrpcLocal(
        val url: String?,
    ) : NetworkMode

    /**
     * Production AWS cloud mode.
     * @param url Optional override URL.
     */
    data class GrpcAws(
        val url: String?,
    ) : NetworkMode
}
