package com.chriscartland.batterybutler.domain.model

/**
 * Feature flags for controlling feature availability at runtime.
 *
 * Features can be enabled/disabled based on:
 * - Platform (Android, iOS, Desktop)
 * - Build type (debug, release)
 * - Remote configuration
 * - User settings
 */
enum class FeatureFlag {
    /**
     * AI-powered batch import feature.
     * Requires platform AI capabilities (e.g., Gemini on Android).
     */
    AI_BATCH_IMPORT,

    /**
     * Remote sync with server.
     * Requires server configuration.
     */
    REMOTE_SYNC,
}
