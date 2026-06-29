package com.chriscartland.batterybutler.domain.model

/**
 * Wraps the Labs **staging** host URL so it can be injected without ambiguity.
 * Blank when unconfigured (owner setup pending).
 */
data class LabsStagingUrl(
    val url: String,
)
