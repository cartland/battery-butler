package com.chriscartland.batterybutler.data.repository

import com.chriscartland.batterybutler.datalocal.preferences.PreferencesDataSource
import com.chriscartland.batterybutler.domain.model.DisplayDensity
import com.chriscartland.batterybutler.domain.repository.DisplayDensityRepository
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.distinctUntilChanged
import kotlinx.coroutines.flow.map
import me.tatarka.inject.annotations.Inject

/**
 * DataStore-backed implementation of [DisplayDensityRepository].
 * Persists the app-wide list density across app restarts.
 */
@Inject
class DataStoreDisplayDensityRepository(
    private val preferencesDataSource: PreferencesDataSource,
) : DisplayDensityRepository {
    // `distinctUntilChanged` for the same reason as DataStoreDataModeRepository: DataStore.data
    // re-emits the WHOLE preferences object on every edit to ANY key, and this store is shared with
    // the data mode + Labs session + refresh-token entries. Without it, signing in would re-emit a
    // structurally-identical density and recompose every list for no reason.
    override val displayDensity: Flow<DisplayDensity> = preferencesDataSource.displayDensityValue
        .map { value -> value.toDisplayDensity() }
        .distinctUntilChanged()

    override suspend fun setDisplayDensity(density: DisplayDensity) {
        preferencesDataSource.setDisplayDensityValue(density.toStorageValue())
    }

    private companion object {
        // Stored as a lowercase token rather than `Enum.name` or `ordinal`. `name` would couple the
        // on-disk format to a Kotlin identifier, and `ordinal` would silently remap every install's
        // saved value if a constant is ever inserted in the middle of the enum.
        private const val VALUE_UNSPECIFIED = "unspecified"
        private const val VALUE_COMPACT = "compact"
        private const val VALUE_EXPANDED = "expanded"

        // An unreadable or unknown value degrades to UNSPECIFIED, which resolves to the default
        // density -- a corrupt entry should look like a fresh install, never a crash.
        fun String?.toDisplayDensity(): DisplayDensity =
            when (this) {
                VALUE_COMPACT -> DisplayDensity.COMPACT
                VALUE_EXPANDED -> DisplayDensity.EXPANDED
                else -> DisplayDensity.UNSPECIFIED
            }

        fun DisplayDensity.toStorageValue(): String =
            when (this) {
                DisplayDensity.UNSPECIFIED -> VALUE_UNSPECIFIED
                DisplayDensity.COMPACT -> VALUE_COMPACT
                DisplayDensity.EXPANDED -> VALUE_EXPANDED
            }
    }
}
