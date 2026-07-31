package com.chriscartland.batterybutler.testcommon

import com.chriscartland.batterybutler.domain.model.DisplayDensity
import com.chriscartland.batterybutler.domain.repository.DisplayDensityRepository
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.MutableStateFlow

/**
 * Fake implementation of [DisplayDensityRepository] for testing.
 *
 * Backed by a [MutableStateFlow] for reactive updates. Defaults to
 * [DisplayDensity.UNSPECIFIED] — the fresh-install value — so tests exercise the
 * resolve-to-default path unless they opt into a concrete density.
 *
 * Example usage:
 * ```kotlin
 * val repo = FakeDisplayDensityRepository()
 * repo.setDisplayDensity(DisplayDensity.COMPACT)
 * assertEquals(DisplayDensity.COMPACT, repo.displayDensity.first())
 * ```
 */
class FakeDisplayDensityRepository(
    initialDensity: DisplayDensity = DisplayDensity.UNSPECIFIED,
) : DisplayDensityRepository {
    private val _displayDensity = MutableStateFlow(initialDensity)

    override val displayDensity: Flow<DisplayDensity> = _displayDensity

    override suspend fun setDisplayDensity(density: DisplayDensity) {
        _displayDensity.value = density
    }
}
