package com.chriscartland.batterybutler.experimental.datalocal

import androidx.datastore.preferences.core.PreferenceDataStoreFactory
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.test.runTest
import okio.Path.Companion.toPath
import java.io.File
import kotlin.test.AfterTest
import kotlin.test.Test
import kotlin.test.assertEquals

/**
 * Integration test for [DataStoreCounterDataSource] — verifies the real DataStore
 * implementation rather than relying solely on [FakeLocalCounterDataSource].
 */
class DataStoreCounterDataSourceTest {
    private val testFile = File.createTempFile("test_datastore_", ".preferences_pb")

    private val dataStore = PreferenceDataStoreFactory.createWithPath(
        produceFile = { testFile.absolutePath.toPath() },
    )

    private val dataSource = DataStoreCounterDataSource(dataStore)

    @AfterTest
    fun tearDown() {
        testFile.delete()
    }

    @Test
    fun `getCounter returns zero initially`() =
        runTest {
            assertEquals(0L, dataSource.getCounter())
        }

    @Test
    fun `incrementCounter returns incremented value`() =
        runTest {
            assertEquals(1L, dataSource.incrementCounter())
            assertEquals(2L, dataSource.incrementCounter())
            assertEquals(3L, dataSource.incrementCounter())
        }

    @Test
    fun `getCounter returns value after increment`() =
        runTest {
            dataSource.incrementCounter()
            dataSource.incrementCounter()
            assertEquals(2L, dataSource.getCounter())
        }

    @Test
    fun `observeCounter emits initial zero`() =
        runTest {
            val value = dataSource.observeCounter().first()
            assertEquals(0L, value)
        }

    @Test
    fun `observeCounter emits updated values after increment`() =
        runTest {
            dataSource.incrementCounter()
            dataSource.incrementCounter()
            val value = dataSource.observeCounter().first()
            assertEquals(2L, value)
        }
}
