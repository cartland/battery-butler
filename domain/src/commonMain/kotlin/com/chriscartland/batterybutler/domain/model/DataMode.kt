package com.chriscartland.batterybutler.domain.model

/**
 * Represents which backend/data source the application reads and writes to.
 *
 * @property Mock Runs completely offline with simulated data.
 * @property GrpcLocal Connects to a locally running gRPC server (e.g., localhost).
 * @property GrpcAws Connects to the production AWS gRPC environment.
 */
sealed interface DataMode {
    /**
     * Offline mock mode using static fixtures.
     */
    data object Mock : DataMode

    /**
     * No backend. App operates locally only ("Device only").
     */
    data object None : DataMode

    /**
     * Local development server mode.
     * @param url Optional override URL.
     */
    data class GrpcLocal(
        val url: String?,
    ) : DataMode

    /**
     * Production AWS cloud mode.
     * @param url Optional override URL.
     */
    data class GrpcAws(
        val url: String?,
    ) : DataMode

    /**
     * Dev server mode.
     * @param url Optional override URL.
     */
    data class GrpcDev(
        val url: String?,
    ) : DataMode

    /**
     * Labs backend, staging channel — REST over HTTPS.
     * @param url Backend host, injected from secret config; null/blank = unavailable.
     */
    data class LabsStaging(
        val url: String?,
    ) : DataMode

    /**
     * Labs backend, production channel — REST over HTTPS.
     * @param url Backend host, injected from secret config; null/blank = unavailable.
     */
    data class LabsProd(
        val url: String?,
    ) : DataMode
}
