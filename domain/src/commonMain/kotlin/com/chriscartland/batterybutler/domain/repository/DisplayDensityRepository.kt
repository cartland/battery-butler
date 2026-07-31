package com.chriscartland.batterybutler.domain.repository

import com.chriscartland.batterybutler.domain.model.DisplayDensity
import kotlinx.coroutines.flow.Flow

/**
 * Persists the app-wide list density preference.
 *
 * Emits the **stored** value, including [DisplayDensity.UNSPECIFIED] for a fresh install —
 * resolving that to a concrete density is the caller's job (see [DisplayDensity.orDefault]), so
 * a screen that wants to show "using the default" can still tell the difference.
 */
interface DisplayDensityRepository {
    val displayDensity: Flow<DisplayDensity>

    suspend fun setDisplayDensity(density: DisplayDensity)
}
