package com.chriscartland.batterybutler.domain.repository

import com.chriscartland.batterybutler.domain.model.FeatureFlag
import kotlinx.coroutines.flow.Flow

/**
 * Provides feature flag availability information.
 *
 * Implementations can determine availability based on:
 * - Platform capabilities
 * - Build configuration
 * - Remote configuration
 * - User preferences
 */
interface FeatureFlagProvider {
    /**
     * Check if a feature is currently enabled.
     */
    fun isEnabled(flag: FeatureFlag): Boolean

    /**
     * Observe feature flag changes over time.
     * Useful for features that can be toggled at runtime.
     */
    fun observeEnabled(flag: FeatureFlag): Flow<Boolean>
}
