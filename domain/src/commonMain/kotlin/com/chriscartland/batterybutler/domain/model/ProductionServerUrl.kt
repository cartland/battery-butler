package com.chriscartland.batterybutler.domain.model

/**
 * Wraps the production server URL so it can be injected without ambiguity.
 */
data class ProductionServerUrl(
    val url: String,
)
