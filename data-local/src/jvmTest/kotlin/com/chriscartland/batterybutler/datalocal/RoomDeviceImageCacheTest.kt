package com.chriscartland.batterybutler.datalocal

import com.chriscartland.batterybutler.datalocal.room.DatabaseFactory
import com.chriscartland.batterybutler.datalocal.room.DatabaseOption
import com.chriscartland.batterybutler.datalocal.room.DynamicDatabaseProvider
import com.chriscartland.batterybutler.domain.model.DataMode
import com.chriscartland.batterybutler.domain.model.DeviceImageBytes
import com.chriscartland.batterybutler.domain.repository.DataModeRepository
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.cancel
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.test.StandardTestDispatcher
import kotlinx.coroutines.test.TestScope
import kotlinx.coroutines.test.advanceUntilIdle
import kotlinx.coroutines.test.runTest
import java.io.File
import kotlin.test.AfterTest
import kotlin.test.BeforeTest
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertNull
import kotlin.test.assertTrue

@OptIn(ExperimentalCoroutinesApi::class)
class RoomDeviceImageCacheTest {
    private val tmpDir = File(System.getProperty("java.io.tmpdir"))

    @BeforeTest
    @AfterTest
    fun cleanDbFiles() {
        DatabaseOption.baseFileNames.values.forEach { File(tmpDir, it).delete() }
    }

    /**
     * Builds a cache backed by a real [DynamicDatabaseProvider] (like production), starting in
     * [initialMode]. Returns the cache, the mode repo (to drive mode switches), and the provider
     * scope (cancel it at the end of the test).
     */
    private fun TestScope.newCache(initialMode: DataMode = DataMode.None): Triple<RoomDeviceImageCache, FakeDataModeRepo, CoroutineScope> {
        val providerScope = CoroutineScope(SupervisorJob() + StandardTestDispatcher(testScheduler))
        val modeRepo = FakeDataModeRepo(initialMode)
        val provider = DynamicDatabaseProvider(
            factory = DatabaseFactory(),
            dataModeRepository = modeRepo,
            scope = providerScope,
        )
        advanceUntilIdle()
        return Triple(RoomDeviceImageCache(provider), modeRepo, providerScope)
    }

    @Test
    fun `get returns null for an uncached etag`() =
        runTest {
            val (store, _, scope) = newCache()
            assertNull(store.get("missing"))
            scope.cancel()
        }

    @Test
    fun `put then get round-trips bytes and content type`() =
        runTest {
            val (store, _, scope) = newCache()
            store.put("etag-1", DeviceImageBytes(byteArrayOf(1, 2, 3), "image/jpeg"))

            val result = store.get("etag-1")

            assertEquals(listOf<Byte>(1, 2, 3), result?.bytes?.toList())
            assertEquals("image/jpeg", result?.contentType)
            scope.cancel()
        }

    @Test
    fun `put overwrites an existing entry for the same etag`() =
        runTest {
            val (store, _, scope) = newCache()
            store.put("etag-1", DeviceImageBytes(byteArrayOf(1), "image/jpeg"))
            store.put("etag-1", DeviceImageBytes(byteArrayOf(2), "image/png"))

            val result = store.get("etag-1")

            assertEquals(listOf<Byte>(2), result?.bytes?.toList())
            assertEquals("image/png", result?.contentType)
            scope.cancel()
        }

    @Test
    fun `evictExcept drops entries not in the keep set`() =
        runTest {
            val (store, _, scope) = newCache()
            store.put("keep", DeviceImageBytes(byteArrayOf(1), "image/jpeg"))
            store.put("drop", DeviceImageBytes(byteArrayOf(2), "image/jpeg"))

            store.evictExcept(setOf("keep"))

            assertTrue(store.get("keep") != null)
            assertNull(store.get("drop"))
            scope.cancel()
        }

    @Test
    fun `evictExcept with an empty keep set drops everything`() =
        runTest {
            val (store, _, scope) = newCache()
            store.put("a", DeviceImageBytes(byteArrayOf(1), "image/jpeg"))
            store.put("b", DeviceImageBytes(byteArrayOf(2), "image/jpeg"))

            store.evictExcept(emptySet())

            assertNull(store.get("a"))
            assertNull(store.get("b"))
            scope.cancel()
        }

    @Test
    fun `observe follows the active database across a mode switch instead of wedging on a closed one`() =
        runTest {
            val (store, modeRepo, scope) = newCache(DataMode.None)

            // Switch away from OFFLINE: DynamicDatabaseProvider swaps in the Labs db and close()s +
            // evicts the OFFLINE instance. Pre-fix, the cache still held that closed OFFLINE db and
            // observe() completed with NO emission -> the detail screen wedged at Loading forever.
            modeRepo.setDataMode(DataMode.LabsProd(url = "https://labs.example"))
            advanceUntilIdle()

            store.put("etag-1", DeviceImageBytes(byteArrayOf(9), "image/png"))

            // The first non-null emission proves observe() is live on the active db, not the closed
            // one. (A regression here would hang this test rather than fail an assertion.)
            val observed = store.observe("etag-1").first { it != null }
            assertEquals(listOf<Byte>(9), observed?.bytes?.toList())
            assertEquals("image/png", observed?.contentType)

            scope.cancel()
        }

    private class FakeDataModeRepo(
        initial: DataMode,
    ) : DataModeRepository {
        private val mode = MutableStateFlow(initial)
        override val dataMode: Flow<DataMode> = mode

        override suspend fun setDataMode(mode: DataMode) {
            this.mode.value = mode
        }
    }
}
