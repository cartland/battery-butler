package com.chriscartland.batterybutler.domain.model

/**
 * Wraps the Labs **production** host URL so it can be injected without ambiguity.
 * Blank when unconfigured (owner setup pending).
 */
data class LabsProdUrl(
    val url: String,
)
