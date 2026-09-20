package com.chriscartland.batterybutler.data.repository

import com.chriscartland.batterybutler.datalocal.preferences.PreferencesDataSource
import com.chriscartland.batterybutler.domain.model.ListArrangement
import com.chriscartland.batterybutler.domain.model.ListScreen
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.test.runTest
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertNull
import kotlin.test.assertTrue

class DataStoreListArrangementRepositoryTest {
    private class InMemoryPreferences : PreferencesDataSource {
        val values = MutableStateFlow<Map<String, String>>(emptyMap())

        override val dataModeValue: Flow<String?> = MutableStateFlow(null)

        override suspend fun setDataModeValue(value: String) = Unit

        override val displayDensityValue: Flow<String?> = MutableStateFlow(null)

        override suspend fun setDisplayDensityValue(value: String) = Unit

        override fun stringValue(key: String): Flow<String?> = values.map { it[key] }

        override suspend fun setStringValue(
            key: String,
            value: String,
        ) {
            values.value = values.value + (key to value)
        }
    }

    @Test
    fun `a fresh install reads as all-null`() =
        runTest {
            val repo = DataStoreListArrangementRepository(InMemoryPreferences())

            val arrangement = repo.arrangement(ListScreen.DEVICES).first()

            assertEquals(ListArrangement(), arrangement)
        }

    @Test
    fun `a saved arrangement reads back`() =
        runTest {
            val repo = DataStoreListArrangementRepository(InMemoryPreferences())

            repo.setArrangement(
                ListScreen.DEVICES,
                ListArrangement("battery_age", "location", isSortAscending = false, isGroupAscending = true),
            )

            assertEquals(
                ListArrangement("battery_age", "location", isSortAscending = false, isGroupAscending = true),
                repo.arrangement(ListScreen.DEVICES).first(),
            )
        }

    /** Each list keeps its own arrangement -- sorting devices says nothing about device types. */
    @Test
    fun `the two list screens do not share an arrangement`() =
        runTest {
            val repo = DataStoreListArrangementRepository(InMemoryPreferences())

            repo.setArrangement(ListScreen.DEVICES, ListArrangement(sortKey = "battery_age"))

            assertEquals("battery_age", repo.arrangement(ListScreen.DEVICES).first().sortKey)
            assertNull(repo.arrangement(ListScreen.DEVICE_TYPES).first().sortKey)
        }

    @Test
    fun `both booleans round-trip in both states`() =
        runTest {
            val repo = DataStoreListArrangementRepository(InMemoryPreferences())

            repo.setArrangement(
                ListScreen.DEVICE_TYPES,
                ListArrangement(isSortAscending = true, isGroupAscending = false),
            )

            val stored = repo.arrangement(ListScreen.DEVICE_TYPES).first()
            assertEquals(true, stored.isSortAscending)
            assertEquals(false, stored.isGroupAscending)
        }

    /** A null field means "not chosen", so writing one must not wipe what is already stored. */
    @Test
    fun `writing a partial arrangement leaves the other fields alone`() =
        runTest {
            val repo = DataStoreListArrangementRepository(InMemoryPreferences())
            repo.setArrangement(ListScreen.DEVICES, ListArrangement(sortKey = "name", groupKey = "type"))

            repo.setArrangement(ListScreen.DEVICES, ListArrangement(sortKey = "location"))

            val stored = repo.arrangement(ListScreen.DEVICES).first()
            assertEquals("location", stored.sortKey)
            assertEquals("type", stored.groupKey)
        }

    /** A hand-edited or downgraded store must degrade to "not chosen", never crash. */
    @Test
    fun `an unparseable boolean reads as not chosen`() =
        runTest {
            val prefs = InMemoryPreferences()
            prefs.setStringValue("list_arrangement.devices.sort_ascending", "yes-please")
            val repo = DataStoreListArrangementRepository(prefs)

            assertNull(repo.arrangement(ListScreen.DEVICES).first().isSortAscending)
        }

    @Test
    fun `each screen writes under its own key prefix`() =
        runTest {
            val prefs = InMemoryPreferences()
            val repo = DataStoreListArrangementRepository(prefs)

            repo.setArrangement(ListScreen.DEVICE_TYPES, ListArrangement(sortKey = "name"))

            assertTrue(
                prefs.values.value.keys
                    .all { it.startsWith("list_arrangement.device_types.") },
            )
        }
}
