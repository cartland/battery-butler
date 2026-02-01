package com.chriscartland.batterybutler.ai

/**
 * Configuration for AI engine initialization.
 *
 * This interface abstracts the source of AI configuration (API keys, model settings, etc.)
 * allowing different implementations for different environments:
 * - Production: Read from secure storage or server
 * - Development: Read from local.properties via BuildConfig
 * - Testing: Provide mock configuration
 */
interface AiConfig {
    /**
     * The API key for the AI service.
     * Returns empty string if not configured.
     */
    val apiKey: String
}
