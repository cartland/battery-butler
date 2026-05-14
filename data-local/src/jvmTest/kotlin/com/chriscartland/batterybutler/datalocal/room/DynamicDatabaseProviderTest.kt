package com.chriscartland.batterybutler.datalocal.room

import com.chriscartland.batterybutler.datalocal.room.entity.toEntity
import com.chriscartland.batterybutler.domain.model.DeviceType
import com.chriscartland.batterybutler.domain.model.NetworkMode
import com.chriscartland.batterybutler.domain.repository.NetworkModeRepository
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.cancel
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.launch
import kotlinx.coroutines.test.StandardTestDispatcher
import kotlinx.coroutines.test.advanceUntilIdle
import kotlinx.coroutines.test.runTest
import java.io.File
import java.util.UUID
import kotlin.test.AfterTest
import kotlin.test.BeforeTest
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertTrue

/**
 * Tests for [DynamicDatabaseProvider.restoreFromLegacy] — specifically the
 * rebind-signal contract that fixes bd issue bb-lg42.
 *
 * Repro: after `restoreFromLegacy` copies a legacy SQLite file over the active
 * DB filename and creates a new Room instance, downstream `Flow` collectors
 * built on `databaseProvider.database.flatMapLatest { ... }` must see fresh
 * data. Before the fix, the swap could silently fail to propagate to some
 * collectors, leaving the UI stuck on `Loading`.
 */
class DynamicDatabaseProviderTest {
    private val tmpDir = File(System.getProperty("java.io.tmpdir"))

    @BeforeTest
    @AfterTest
    fun cleanDbFiles() {
        DatabaseOption.entries.forEach { File(tmpDir, it.fileName).delete() }
        DatabaseOption.legacyFileNames.values.forEach { File(tmpDir, it).delete() }
    }

    @Test
    fun `rebindSignal emits when restoreFromLegacy succeeds`() =
        runTest {
            val dispatcher = StandardTestDispatcher(testScheduler)
            val providerScope = CoroutineScope(SupervisorJob() + dispatcher)
            val factory = DatabaseFactory()
            val provider = DynamicDatabaseProvider(
                factory = factory,
                networkModeRepository = FakeNetworkModeRepo(NetworkMode.None),
                scope = providerScope,
            )

            // Seed the legacy file at the legacy filename. Trick: use the regular
            // factory path to write to the Offline DB, rename to legacy, evict.
            val activeOption = DatabaseOption.Offline
            val legacyFileName = DatabaseOption.legacyFileNames[activeOption]!!
            val legacyType = DeviceType(
                id = UUID.randomUUID().toString(),
                name = "Legacy Type",
                batteryType = "AA",
                defaultIcon = null,
            )
            seedLegacyFile(factory, activeOption, legacyFileName, listOf(legacyType))

            val signals = mutableListOf<Long>()
            val job = launch(dispatcher) { provider.rebindSignal.collect { signals.add(it) } }
            advanceUntilIdle()
            assertTrue(signals.isEmpty(), "rebindSignal should not emit before restore")

            provider.restoreFromLegacy(legacyFileName)
            advanceUntilIdle()

            assertEquals(1, signals.size, "rebindSignal should emit exactly once per restore")
            assertEquals(1L, signals[0], "first restore should emit version 1")

            provider.restoreFromLegacy(legacyFileName)
            advanceUntilIdle()
            assertEquals(2, signals.size, "second restore should emit again")
            assertEquals(2L, signals[1], "rebindSignal counter must monotonically increase")

            job.cancel()
            providerScope.cancel()
        }

    /**
     * Populates a SQLite file at `legacyFileName` by reusing `activeOption`'s
     * normal DB path, then renaming. After return: `legacyFileName` exists,
     * `activeOption.fileName` does not, factory cache is clean.
     */
    private suspend fun seedLegacyFile(
        factory: DatabaseFactory,
        activeOption: DatabaseOption,
        legacyFileName: String,
        types: List<DeviceType>,
    ) {
        val seedDb = factory.createDatabase(activeOption)
        types.forEach { seedDb.deviceDao().insertDeviceType(it.toEntity()) }
        seedDb.close()
        factory.evict(activeOption)

        val source = File(tmpDir, activeOption.fileName)
        val legacy = File(tmpDir, legacyFileName)
        require(source.exists()) { "expected seed file at ${source.absolutePath}" }
        source.renameTo(legacy)
    }

    private class FakeNetworkModeRepo(
        initial: NetworkMode,
    ) : NetworkModeRepository {
        private val mode = MutableStateFlow(initial)
        override val networkMode: Flow<NetworkMode> = mode

        override suspend fun setNetworkMode(m: NetworkMode) {
            mode.value = m
        }
    }
}
