package com.chriscartland.batterybutler.data.repository

import com.chriscartland.batterybutler.datalocal.preferences.PreferencesDataSource
import com.chriscartland.batterybutler.domain.model.DisplayDensity
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.flowOf
import kotlinx.coroutines.flow.toList
import kotlinx.coroutines.test.runTest
import kotlin.test.Test
import kotlin.test.assertEquals

class DataStoreDisplayDensityRepositoryTest {
    private fun repositoryOver(values: Flow<String?>): DataStoreDisplayDensityRepository =
        DataStoreDisplayDensityRepository(
            preferencesDataSource = FakeDensityPreferencesDataSource(values),
        )

    @Test
    fun `no stored value reads as UNSPECIFIED`() =
        runTest {
            val densities = repositoryOver(flowOf(null)).displayDensity.toList()
            assertEquals(listOf(DisplayDensity.UNSPECIFIED), densities)
        }

    @Test
    fun `stored tokens map back to their enum values`() =
        runTest {
            val densities = repositoryOver(flowOf("compact", "expanded", "unspecified")).displayDensity.toList()
            assertEquals(
                listOf(DisplayDensity.COMPACT, DisplayDensity.EXPANDED, DisplayDensity.UNSPECIFIED),
                densities,
            )
        }

    // A value written by a newer build - or a corrupted entry - must look like a fresh install
    // rather than crash. UNSPECIFIED resolves to the default density downstream.
    @Test
    fun `unrecognised stored value degrades to UNSPECIFIED`() =
        runTest {
            val densities = repositoryOver(flowOf("ultra-dense")).displayDensity.toList()
            assertEquals(listOf(DisplayDensity.UNSPECIFIED), densities)
        }

    // DataStore re-emits the whole preferences object on any edit, including edits to unrelated
    // keys in the same store (data mode, Labs session, refresh token). Without
    // distinctUntilChanged those show up here as redundant identical emissions.
    @Test
    fun `repeated identical values collapse to a single emission`() =
        runTest {
            val densities = repositoryOver(flowOf("compact", "compact", "compact")).displayDensity.toList()
            assertEquals(listOf(DisplayDensity.COMPACT), densities)
        }

    @Test
    fun `setDisplayDensity writes the round-trippable token`() =
        runTest {
            val fake = FakeDensityPreferencesDataSource(flowOf(null))
            val repository = DataStoreDisplayDensityRepository(preferencesDataSource = fake)

            repository.setDisplayDensity(DisplayDensity.COMPACT)

            assertEquals("compact", fake.written)
            // Round-trip: what was written must read back as what was set.
            assertEquals(
                listOf(DisplayDensity.COMPACT),
                repositoryOver(flowOf(fake.written)).displayDensity.toList(),
            )
        }

    @Test
    fun `UNSPECIFIED resolves to EXPANDED and COMPACT is compact`() {
        assertEquals(DisplayDensity.EXPANDED, DisplayDensity.UNSPECIFIED.orDefault())
        assertEquals(DisplayDensity.COMPACT, DisplayDensity.COMPACT.orDefault())
        assertEquals(false, DisplayDensity.UNSPECIFIED.isCompact)
        assertEquals(true, DisplayDensity.COMPACT.isCompact)
    }
}

/** A [PreferencesDataSource] whose density flow is supplied directly by the test. */
private class FakeDensityPreferencesDataSource(
    override val displayDensityValue: Flow<String?>,
) : PreferencesDataSource {
    var written: String? = null
        private set

    override val dataModeValue: Flow<String?> = flowOf(null)

    override suspend fun setDataModeValue(value: String) = Unit

    override suspend fun setDisplayDensityValue(value: String) {
        written = value
    }
}
