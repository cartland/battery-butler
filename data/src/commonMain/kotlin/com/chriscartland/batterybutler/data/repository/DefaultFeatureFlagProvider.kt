package com.chriscartland.batterybutler.data.repository

import com.chriscartland.batterybutler.domain.model.FeatureFlag
import com.chriscartland.batterybutler.domain.repository.FeatureFlagProvider
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.MutableStateFlow
import me.tatarka.inject.annotations.Inject

/**
 * Default implementation of [FeatureFlagProvider].
 *
 * Takes a set of enabled features at construction time.
 * Platform-specific code determines which features are available.
 */
@Inject
class DefaultFeatureFlagProvider(
    private val enabledFeatures: Set<FeatureFlag>,
) : FeatureFlagProvider {
    private val featureStates = FeatureFlag.entries.associateWith { flag ->
        MutableStateFlow(enabledFeatures.contains(flag))
    }

    override fun isEnabled(flag: FeatureFlag): Boolean = enabledFeatures.contains(flag)

    override fun observeEnabled(flag: FeatureFlag): Flow<Boolean> = featureStates[flag] ?: MutableStateFlow(false)
}
