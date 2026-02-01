package com.chriscartland.batterybutler.config

import com.chriscartland.batterybutler.ai.AiConfig
import com.chriscartland.batterybutler.composeapp.BuildConfig

/**
 * AiConfig implementation that reads from BuildConfig.
 *
 * This centralizes API key access to compose-app module only.
 * The key is still read from local.properties at build time, but this
 * abstraction makes it easier to migrate to runtime configuration later
 * (e.g., reading from encrypted storage, server-provided config, or user settings).
 */
class BuildConfigAiConfig : AiConfig {
    override val apiKey: String = BuildConfig.GEMINI_API_KEY
}
