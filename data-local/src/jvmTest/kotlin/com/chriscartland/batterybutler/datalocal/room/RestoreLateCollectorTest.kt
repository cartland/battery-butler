package com.chriscartland.batterybutler.datalocal.room

import com.chriscartland.batterybutler.datalocal.RoomLocalDataSource
import com.chriscartland.batterybutler.datalocal.room.entity.toEntity
import com.chriscartland.batterybutler.domain.model.DataMode
import com.chriscartland.batterybutler.domain.model.DeviceType
import com.chriscartland.batterybutler.domain.repository.DataModeRepository
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.cancel
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.launch
import kotlinx.coroutines.runBlocking
import kotlinx.coroutines.withTimeout
import java.io.File
import java.util.UUID
import kotlin.test.AfterTest
import kotlin.test.BeforeTest
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertTrue

/**
 * End-to-end regression test for `bb-lg42`: after a legacy-database restore, a Room-backed Flow
 * that was **not** being collected at the time of the restore must still deliver the restored data
 * when something subscribes afterwards.
 *
 * WHY THIS EXISTS
 *
 * android/30 shipped the bb-lg42 bug — after "Restore Previous Data" the Device Types and History
 * tabs sat on Loading until the app was restarted — and android/31 was needed two days later. The
 * response at the time was a manual smoke-test checklist. This test replaces that checklist item:
 * the failure is deterministic and cheap to reproduce in process, so it does not need a human with
 * a device.
 *
 * WHY THE EXISTING TEST WASN'T ENOUGH
 *
 * [DynamicDatabaseProviderTest] asserts that `rebindSignal` emits during `restoreFromLegacy`. That
 * is the *mechanism*, not the *symptom*. It passes even if a late subscriber binds to the stale
 * database, because the signal fired regardless of who was listening — which is exactly how
 * android/31 shipped with the symptom still present (see `bb-qz7w`).
 *
 * The scenario below is the one that actually broke: the tab's `WhileSubscribed` flow had already
 * timed out, so at the moment of the restore there was **no collector at all**.
 */
class RestoreLateCollectorTest {
    private companion object {
        // Generous: these are real Room I/O operations, not virtual time. Long enough that a
        // slow CI runner never flakes, short enough that a genuine hang still fails the build.
        const val TIMEOUT_MS = 30_000L
    }

    private val tmpDir = File(System.getProperty("java.io.tmpdir"))

    @BeforeTest
    @AfterTest
    fun cleanDbFiles() {
        DatabaseOption.baseFileNames.values.forEach { File(tmpDir, it).delete() }
        DatabaseOption.legacyFileNames.values.forEach { File(tmpDir, it).delete() }
    }

    @Test
    fun `a collector that subscribes after a restore sees the restored data`() =
        // Real time, real dispatchers: Room runs queries and invalidation on its own executors,
        // which a TestScheduler's virtual clock does not drive. runTest+advanceUntilIdle would
        // simply never see an emission -- the reason this scenario had no coverage.
        runBlocking {
            val providerScope = CoroutineScope(SupervisorJob() + Dispatchers.Default)
            val factory = DatabaseFactory()

            val legacyType = DeviceType(
                id = UUID.randomUUID().toString(),
                name = "Legacy Smoke Detector",
                batteryType = "9V",
                defaultIcon = null,
            )
            val legacyFileName = DatabaseOption.legacyFileNames[DatabaseOption.OFFLINE.category]!!
            // Seed BEFORE constructing the provider. seedLegacyFile closes and renames the OFFLINE
            // database file, so doing it afterwards would pull the file out from under the
            // provider's eagerly-created instance and no query would ever emit.
            seedLegacyFile(factory, DatabaseOption.OFFLINE, legacyFileName, listOf(legacyType))

            val provider = DynamicDatabaseProvider(
                factory = factory,
                dataModeRepository = FakeDataModeRepo(DataMode.None),
                scope = providerScope,
            )
            val localDataSource = RoomLocalDataSource(provider)

            // 1. Subscribe, observe the (empty) live database, then unsubscribe. This is the
            //    `WhileSubscribed` timeout: the tab was backgrounded long enough for its flow to
            //    stop, so nothing is listening when the restore happens.
            val firstEmission = withTimeout(TIMEOUT_MS) {
                localDataSource.getAllDeviceTypes().first()
            }
            assertTrue(
                firstEmission.isEmpty(),
                "expected an empty live database before the restore, got $firstEmission",
            )
            // `first()` already cancelled the collection, so nothing is listening from here on --
            // exactly the WhileSubscribed-timed-out state.

            // 2. Restore with NO active collector.
            provider.restoreFromLegacy(legacyFileName)

            // 3. Re-subscribe, as the tab does when it comes back to the foreground. This must
            //    deliver the restored row rather than hanging or replaying the pre-restore empty
            //    list -- the latter is the bb-lg42 "stuck on Loading" symptom.
            val restored = withTimeout(TIMEOUT_MS) {
                localDataSource.getAllDeviceTypes().first { it.isNotEmpty() }
            }

            assertEquals(1, restored.size, "expected exactly the restored device type")
            assertEquals(legacyType.name, restored.single().name)

            providerScope.cancel()
        }

    /**
     * The mirror case, which already worked and must keep working: a collector active *across* the
     * restore. Kept alongside the late-collector test so a future change cannot fix one by
     * breaking the other.
     */
    @Test
    fun `a collector active across a restore sees the restored data`() =
        runBlocking {
            val providerScope = CoroutineScope(SupervisorJob() + Dispatchers.Default)
            val factory = DatabaseFactory()

            val legacyType = DeviceType(
                id = UUID.randomUUID().toString(),
                name = "Legacy Thermostat",
                batteryType = "AA",
                defaultIcon = null,
            )
            val legacyFileName = DatabaseOption.legacyFileNames[DatabaseOption.OFFLINE.category]!!
            // Seed BEFORE constructing the provider. seedLegacyFile closes and renames the OFFLINE
            // database file, so doing it afterwards would pull the file out from under the
            // provider's eagerly-created instance and no query would ever emit.
            seedLegacyFile(factory, DatabaseOption.OFFLINE, legacyFileName, listOf(legacyType))

            val provider = DynamicDatabaseProvider(
                factory = factory,
                dataModeRepository = FakeDataModeRepo(DataMode.None),
                scope = providerScope,
            )
            val localDataSource = RoomLocalDataSource(provider)

            val seen = java.util.concurrent.CopyOnWriteArrayList<List<DeviceType>>()
            val job = launch(Dispatchers.Default) {
                localDataSource.getAllDeviceTypes().collect { seen.add(it) }
            }
            withTimeout(TIMEOUT_MS) { localDataSource.getAllDeviceTypes().first() }

            provider.restoreFromLegacy(legacyFileName)

            withTimeout(TIMEOUT_MS) {
                localDataSource.getAllDeviceTypes().first { it.isNotEmpty() }
            }
            assertTrue(
                seen.any { types -> types.any { it.name == legacyType.name } },
                "collector active across the restore never saw the restored data; saw $seen",
            )

            job.cancel()
            providerScope.cancel()
        }

    /**
     * Populates a SQLite file at `legacyFileName` by reusing `activeOption`'s normal DB path, then
     * renaming. After return: `legacyFileName` exists, `activeOption.fileName` does not, and the
     * factory cache is clean.
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

    private class FakeDataModeRepo(
        initial: DataMode,
    ) : DataModeRepository {
        private val mode = MutableStateFlow(initial)
        override val dataMode: Flow<DataMode> = mode

        override suspend fun setDataMode(m: DataMode) {
            mode.value = m
        }
    }
}
