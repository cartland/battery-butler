package com.chriscartland.batterybutler.data.repository

import com.chriscartland.batterybutler.datalocal.preferences.PreferencesDataSource
import com.chriscartland.batterybutler.domain.model.DataMode
import com.chriscartland.batterybutler.domain.model.LabsProdUrl
import com.chriscartland.batterybutler.domain.model.LabsStagingUrl
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.flowOf
import kotlinx.coroutines.flow.toList
import kotlinx.coroutines.test.runTest
import kotlin.test.Test
import kotlin.test.assertEquals

/**
 * Pins the `distinctUntilChanged()` on [DataStoreDataModeRepository.dataMode].
 *
 * The stored value flow is backed by `DataStore.data`, which re-emits the WHOLE preferences object
 * on every edit to ANY key — and that store is shared with the Labs session + refresh-token
 * persistence. A sign-in therefore writes unrelated keys while the selected mode is unchanged, so
 * without deduping, `dataMode` would emit a structurally-identical [DataMode] repeatedly.
 * `DelegatingRemoteDataSource.subscribe()` does `dataMode.flatMapLatest { … }`, so each such
 * re-emission would cancel and restart the in-flight `/sync` — which, right after sign-out cleared
 * the local DB, could cancel the repopulating snapshot write and leave the device list empty.
 * See `bb-signin-empty-list` in TODO.md.
 */
class DataStoreDataModeRepositoryTest {
    private fun repositoryOver(dataModeValues: Flow<String?>): DataStoreDataModeRepository =
        DataStoreDataModeRepository(
            preferencesDataSource = FakePreferencesDataSource(dataModeValues),
            labsStagingUrl = LabsStagingUrl("https://cartland-labs-staging.web.app"),
            labsProdUrl = LabsProdUrl("https://cartland-labs.web.app"),
        )

    @Test
    fun `repeated identical stored values from the shared DataStore emit dataMode only once`() =
        runTest {
            // Three emissions of the SAME stored value — as the shared DataStore does when a sign-in
            // writes session/refresh-token keys without changing the selected mode.
            val repeated = flowOf(
                "labs_prod:https://cartland-labs.web.app",
                "labs_prod:https://cartland-labs.web.app",
                "labs_prod:https://cartland-labs.web.app",
            )

            val emitted = repositoryOver(repeated).dataMode.toList()

            assertEquals(
                listOf(DataMode.LabsProd("https://cartland-labs.web.app")),
                emitted,
                "Unrelated DataStore writes must not re-emit an unchanged data mode",
            )
        }

    @Test
    fun `real mode changes still propagate and only consecutive duplicates collapse`() =
        runTest {
            val values = flowOf(
                "labs_prod:https://cartland-labs.web.app",
                "labs_staging:https://cartland-labs-staging.web.app",
                "labs_staging:https://cartland-labs-staging.web.app",
            )

            val emitted = repositoryOver(values).dataMode.toList()

            assertEquals(
                listOf(
                    DataMode.LabsProd("https://cartland-labs.web.app"),
                    DataMode.LabsStaging("https://cartland-labs-staging.web.app"),
                ),
                emitted,
                "distinctUntilChanged must collapse only consecutive duplicates, not real changes",
            )
        }
}

/** A [PreferencesDataSource] whose stored value flow is supplied directly by the test. */
private class FakePreferencesDataSource(
    override val dataModeValue: Flow<String?>,
    override val displayDensityValue: Flow<String?> = flowOf(null),
) : PreferencesDataSource {
    override suspend fun setDataModeValue(value: String) = Unit

    override suspend fun setDisplayDensityValue(value: String) = Unit
}
