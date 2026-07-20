package com.chriscartland.batterybutler.data.repository

import com.chriscartland.batterybutler.datanetwork.RemoteDataSource
import com.chriscartland.batterybutler.datanetwork.RemoteDataSourceState
import com.chriscartland.batterybutler.domain.model.BatteryEvent
import com.chriscartland.batterybutler.domain.model.DataError
import com.chriscartland.batterybutler.domain.model.DataMode
import com.chriscartland.batterybutler.domain.model.Device
import com.chriscartland.batterybutler.domain.model.DeviceType
import com.chriscartland.batterybutler.domain.model.SyncStatus
import com.chriscartland.batterybutler.domain.repository.RemoteUpdate
import com.chriscartland.batterybutler.testcommon.FakeDataModeRepository
import com.chriscartland.batterybutler.testcommon.FakeDeviceImageCache
import com.chriscartland.batterybutler.testcommon.FakeDeviceImageDataSource
import com.chriscartland.batterybutler.testcommon.FakeLocalDataSource
import com.chriscartland.batterybutler.testcommon.FakeRemoteDataSource
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.Job
import kotlinx.coroutines.awaitCancellation
import kotlinx.coroutines.cancel
import kotlinx.coroutines.channels.Channel
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.emptyFlow
import kotlinx.coroutines.flow.flow
import kotlinx.coroutines.flow.flowOf
import kotlinx.coroutines.flow.receiveAsFlow
import kotlinx.coroutines.launch
import kotlinx.coroutines.test.StandardTestDispatcher
import kotlinx.coroutines.test.advanceUntilIdle
import kotlinx.coroutines.test.resetMain
import kotlinx.coroutines.test.runTest
import kotlinx.coroutines.test.setMain
import kotlin.test.AfterTest
import kotlin.test.BeforeTest
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertIs
import kotlin.test.assertTrue
import kotlin.time.ExperimentalTime
import kotlin.time.Instant

@OptIn(ExperimentalCoroutinesApi::class, ExperimentalTime::class)
class DefaultDeviceRepositoryTest {
    private val testDispatcher = StandardTestDispatcher()

    @BeforeTest
    fun setUp() {
        Dispatchers.setMain(testDispatcher)
    }

    @AfterTest
    fun tearDown() {
        Dispatchers.resetMain()
    }

    /**
     * Creates a [DefaultDeviceRepository] backed by a real [DefaultSyncManager].
     * The returned scope must be cancelled at the end of the test to stop the subscribe retry loop.
     * The [DefaultSyncManager] is also returned for tests that need direct access.
     */
    private fun createRepo(
        local: FakeLocalDataSource = FakeLocalDataSource(),
        remote: RemoteDataSource = FakeRemoteDataSource(),
        dataMode: FakeDataModeRepository = FakeDataModeRepository(DataMode.Mock),
    ): RepoTestHarness {
        val repoScope = CoroutineScope(testDispatcher + Job())
        val imageCoordinator = DeviceImageSyncCoordinator(FakeDeviceImageDataSource(), FakeDeviceImageCache(), repoScope)
        val syncManager = DefaultSyncManager(local, remote, dataMode, imageCoordinator, repoScope)
        val repo = DefaultDeviceRepository(local, syncManager)
        return RepoTestHarness(repo, syncManager, repoScope)
    }

    // ───────────────────────────────────────────────────────
    // Device CRUD Tests
    // ───────────────────────────────────────────────────────

    @Test
    fun `addDevice saves to local and pushes to remote`() =
        runTest(testDispatcher) {
            val local = FakeLocalDataSource()
            val remote = FakeRemoteDataSource()
            val (repo, _, repoScope) = createRepo(local, remote)
            val device = createDevice(id = "1", name = "Test Device")

            repo.addDevice(device)
            advanceUntilIdle()

            assertEquals(1, local.devices.size)
            assertEquals(device, local.devices[0])
            assertEquals(1, remote.pushedUpdates.size)
            assertEquals(device, remote.pushedUpdates[0].devices.firstOrNull())
            repoScope.cancel()
        }

    @Test
    fun `updateDevice updates local and pushes to remote`() =
        runTest(testDispatcher) {
            val local = FakeLocalDataSource()
            val remote = FakeRemoteDataSource()
            val (repo, _, repoScope) = createRepo(local, remote)
            val device = createDevice(id = "1", name = "Updated Device")

            repo.updateDevice(device)
            advanceUntilIdle()

            assertTrue(local.updatedDevices.contains(device))
            assertEquals(device, remote.pushedUpdates[0].devices.firstOrNull())
            repoScope.cancel()
        }

    @Test
    fun `deleteDevice removes from local`() =
        runTest(testDispatcher) {
            val local = FakeLocalDataSource()
            val remote = FakeRemoteDataSource()
            val (repo, _, repoScope) = createRepo(local, remote)

            repo.deleteDevice("device-1")
            advanceUntilIdle()

            assertTrue(local.deletedDeviceIds.contains("device-1"))
            repoScope.cancel()
        }

    // ───────────────────────────────────────────────────────
    // Device Type CRUD Tests
    // ───────────────────────────────────────────────────────

    @Test
    fun `addDeviceType saves to local and pushes to remote`() =
        runTest(testDispatcher) {
            val local = FakeLocalDataSource()
            val remote = FakeRemoteDataSource()
            val (repo, _, repoScope) = createRepo(local, remote)
            val type = DeviceType(id = "type-1", name = "Smoke Detector")

            repo.addDeviceType(type)
            advanceUntilIdle()

            assertEquals(1, local.deviceTypes.size)
            assertEquals(type, local.deviceTypes[0])
            assertEquals(type, remote.pushedUpdates[0].deviceTypes.firstOrNull())
            repoScope.cancel()
        }

    @Test
    fun `updateDeviceType updates local and pushes to remote`() =
        runTest(testDispatcher) {
            val local = FakeLocalDataSource()
            val remote = FakeRemoteDataSource()
            val (repo, _, repoScope) = createRepo(local, remote)
            val type = DeviceType(id = "type-1", name = "Updated Type")

            repo.updateDeviceType(type)
            advanceUntilIdle()

            assertTrue(local.updatedDeviceTypes.contains(type))
            assertEquals(type, remote.pushedUpdates[0].deviceTypes.firstOrNull())
            repoScope.cancel()
        }

    @Test
    fun `deleteDeviceType removes from local`() =
        runTest(testDispatcher) {
            val local = FakeLocalDataSource()
            val remote = FakeRemoteDataSource()
            val (repo, _, repoScope) = createRepo(local, remote)

            repo.deleteDeviceType("type-1")
            advanceUntilIdle()

            assertTrue(local.deletedDeviceTypeIds.contains("type-1"))
            repoScope.cancel()
        }

    // ───────────────────────────────────────────────────────
    // Battery Event CRUD Tests
    // ───────────────────────────────────────────────────────

    @Test
    fun `addEvent saves to local and pushes to remote`() =
        runTest(testDispatcher) {
            val local = FakeLocalDataSource()
            val remote = FakeRemoteDataSource()
            val (repo, _, repoScope) = createRepo(local, remote)
            val event = createBatteryEvent(id = "event-1", deviceId = "device-1")

            repo.addEvent(event)
            advanceUntilIdle()

            assertEquals(1, local.events.size)
            assertEquals(event, local.events[0])
            assertEquals(event, remote.pushedUpdates[0].events.firstOrNull())
            repoScope.cancel()
        }

    @Test
    fun `updateEvent updates local and pushes to remote`() =
        runTest(testDispatcher) {
            val local = FakeLocalDataSource()
            val remote = FakeRemoteDataSource()
            val (repo, _, repoScope) = createRepo(local, remote)
            val event = createBatteryEvent(id = "event-1", deviceId = "device-1")

            repo.updateEvent(event)
            advanceUntilIdle()

            assertTrue(local.updatedEvents.contains(event))
            assertEquals(event, remote.pushedUpdates[0].events.firstOrNull())
            repoScope.cancel()
        }

    @Test
    fun `deleteEvent removes from local`() =
        runTest(testDispatcher) {
            val local = FakeLocalDataSource()
            val remote = FakeRemoteDataSource()
            val (repo, _, repoScope) = createRepo(local, remote)

            repo.deleteEvent("event-1")
            advanceUntilIdle()

            assertTrue(local.deletedEventIds.contains("event-1"))
            repoScope.cancel()
        }

    // ───────────────────────────────────────────────────────
    // Flow Delegation Tests
    // ───────────────────────────────────────────────────────

    @Test
    fun `getAllDevices returns flow from local data source`() =
        runTest(testDispatcher) {
            val local = FakeLocalDataSource()
            val device = createDevice(id = "1", name = "Test")
            local.setDevicesForFlow(listOf(device))
            val (repo, _, repoScope) = createRepo(local)

            val flow = repo.getAllDevices()
            assertEquals(local.getAllDevices(), flow)
            repoScope.cancel()
        }

    @Test
    fun `getAllDeviceTypes returns flow from local data source`() =
        runTest(testDispatcher) {
            val local = FakeLocalDataSource()
            val type = DeviceType(id = "type-1", name = "Test")
            local.setDeviceTypesForFlow(listOf(type))
            val (repo, _, repoScope) = createRepo(local)

            val flow = repo.getAllDeviceTypes()
            assertEquals(local.getAllDeviceTypes(), flow)
            repoScope.cancel()
        }

    // ───────────────────────────────────────────────────────
    // SyncStatus State Transition Tests
    // ───────────────────────────────────────────────────────

    @Test
    fun `syncStatus transitions to Syncing when push starts`() =
        runTest(testDispatcher) {
            val local = FakeLocalDataSource()
            val remote = FakeRemoteDataSource()
            remote.suspendPush = true
            val (repo, _, repoScope) = createRepo(local, remote)
            val device = createDevice(id = "1", name = "Test Device")

            val states = mutableListOf<SyncStatus>()
            val job = backgroundScope.launch {
                repo.syncStatus.collect { states.add(it) }
            }

            repo.addDevice(device)
            testDispatcher.scheduler.advanceTimeBy(10)

            assertTrue(states.any { it is SyncStatus.Syncing })

            remote.resumePush()
            advanceUntilIdle()

            job.cancel()
            repoScope.cancel()
        }

    @Test
    fun `syncStatus is Failed after push returns false`() =
        runTest(testDispatcher) {
            val local = FakeLocalDataSource()
            val remote = FakeRemoteDataSource()
            remote.shouldFail = true
            val (repo, _, repoScope) = createRepo(local, remote)
            val device = createDevice(id = "1", name = "Test Device")

            repo.addDevice(device)
            advanceUntilIdle()

            val status = repo.syncStatus.value
            assertIs<SyncStatus.Failed>(status)
            assertIs<DataError.Network.PushFailed>(status.error)
            repoScope.cancel()
        }

    @Test
    fun `syncStatus is Success after successful push`() =
        runTest(testDispatcher) {
            val local = FakeLocalDataSource()
            val remote = FakeRemoteDataSource()
            val (repo, _, repoScope) = createRepo(local, remote)
            val device = createDevice(id = "1", name = "Test Device")

            repo.addDevice(device)
            advanceUntilIdle()

            assertEquals(SyncStatus.Success, repo.syncStatus.value)
            repoScope.cancel()
        }

    @Test
    fun `dismissSyncStatus returns status to Idle`() =
        runTest(testDispatcher) {
            val local = FakeLocalDataSource()
            val remote = FakeRemoteDataSource()
            val (repo, _, repoScope) = createRepo(local, remote)
            val device = createDevice(id = "1", name = "Test Device")

            repo.addDevice(device)
            advanceUntilIdle()
            assertEquals(SyncStatus.Success, repo.syncStatus.value)

            repo.dismissSyncStatus()

            assertEquals(SyncStatus.Idle, repo.syncStatus.value)
            repoScope.cancel()
        }

    @Test
    fun `push failure preserves local data`() =
        runTest(testDispatcher) {
            val local = FakeLocalDataSource()
            val remote = FakeRemoteDataSource()
            remote.shouldFail = true
            val (repo, _, repoScope) = createRepo(local, remote)
            val device = createDevice(id = "1", name = "Test Device")

            repo.addDevice(device)
            advanceUntilIdle()

            assertEquals(1, local.devices.size)
            assertEquals(device, local.devices[0])
            assertEquals(1, remote.pushedUpdates.size)
            repoScope.cancel()
        }

    // ───────────────────────────────────────────────────────
    // Data Mode Tests
    // ───────────────────────────────────────────────────────

    @Test
    fun `push is skipped when data mode is None`() =
        runTest(testDispatcher) {
            val local = FakeLocalDataSource()
            val remote = FakeRemoteDataSource()
            val dataMode = FakeDataModeRepository(DataMode.None)
            val (repo, _, repoScope) = createRepo(local, remote, dataMode)
            val device = createDevice(id = "1", name = "Offline Device")

            repo.addDevice(device)
            // Use advanceTimeBy instead of advanceUntilIdle because the subscribe
            // loop polls with delay(5000) in None mode, causing advanceUntilIdle to loop forever.
            testDispatcher.scheduler.advanceTimeBy(100)
            testDispatcher.scheduler.runCurrent()

            // Local save still happens
            assertEquals(1, local.devices.size)
            // Remote push is skipped
            assertEquals(0, remote.pushedUpdates.size)
            repoScope.cancel()
        }

    // ───────────────────────────────────────────────────────
    // Delete Push Tests
    // ───────────────────────────────────────────────────────

    @Test
    fun `deleteDevice pushes deletion ID not full object`() =
        runTest(testDispatcher) {
            val local = FakeLocalDataSource()
            val remote = FakeRemoteDataSource()
            val (repo, _, repoScope) = createRepo(local, remote)

            repo.deleteDevice("device-42")
            advanceUntilIdle()

            assertEquals(1, remote.pushedUpdates.size)
            val pushed = remote.pushedUpdates[0]
            assertEquals(listOf("device-42"), pushed.deletedDeviceIds)
            assertTrue(pushed.devices.isEmpty())
            assertTrue(pushed.events.isEmpty())
            assertTrue(pushed.deviceTypes.isEmpty())
            repoScope.cancel()
        }

    @Test
    fun `deleteDeviceType pushes deletion ID`() =
        runTest(testDispatcher) {
            val local = FakeLocalDataSource()
            val remote = FakeRemoteDataSource()
            val (repo, _, repoScope) = createRepo(local, remote)

            repo.deleteDeviceType("type-99")
            advanceUntilIdle()

            assertEquals(1, remote.pushedUpdates.size)
            val pushed = remote.pushedUpdates[0]
            assertEquals(listOf("type-99"), pushed.deletedDeviceTypeIds)
            assertTrue(pushed.devices.isEmpty())
            repoScope.cancel()
        }

    @Test
    fun `deleteEvent pushes deletion ID`() =
        runTest(testDispatcher) {
            val local = FakeLocalDataSource()
            val remote = FakeRemoteDataSource()
            val (repo, _, repoScope) = createRepo(local, remote)

            repo.deleteEvent("event-77")
            advanceUntilIdle()

            assertEquals(1, remote.pushedUpdates.size)
            val pushed = remote.pushedUpdates[0]
            assertEquals(listOf("event-77"), pushed.deletedEventIds)
            assertTrue(pushed.devices.isEmpty())
            repoScope.cancel()
        }

    // ───────────────────────────────────────────────────────
    // Edge Case Tests
    // ───────────────────────────────────────────────────────

    @Test
    fun `multiple rapid updates do not corrupt state`() =
        runTest(testDispatcher) {
            val local = FakeLocalDataSource()
            val remote = FakeRemoteDataSource()
            val (repo, _, repoScope) = createRepo(local, remote)

            val devices = (1..10).map {
                createDevice(id = "d$it", name = "Device $it")
            }

            // Fire all adds rapidly
            devices.forEach { repo.addDevice(it) }
            advanceUntilIdle()

            assertEquals(10, local.devices.size)
            assertEquals(10, remote.pushedUpdates.size)
            repoScope.cancel()
        }

    @Test
    fun `push exception produces Failed with Unknown error`() =
        runTest(testDispatcher) {
            val local = FakeLocalDataSource()
            val remote = ThrowingRemoteDataSource()
            val (repo, _, repoScope) = createRepo(local, remote)
            val device = createDevice(id = "1", name = "Test")

            repo.addDevice(device)
            advanceUntilIdle()

            assertEquals(1, local.devices.size)
            val status = repo.syncStatus.value
            assertIs<SyncStatus.Failed>(status)
            assertIs<DataError.Unknown>(status.error)
            repoScope.cancel()
        }

    // ───────────────────────────────────────────────────────
    // applyRemoteUpdate Tests (via SyncManager)
    // ───────────────────────────────────────────────────────

    @Test
    fun `applyRemoteUpdate stores data in local data source`() =
        runTest(testDispatcher) {
            val local = FakeLocalDataSource()
            val (_, syncManager, repoScope) = createRepo(local)
            val device = createDevice(id = "d1", name = "Hallway Detector")
            val type = DeviceType(id = "t1", name = "Smoke Detector")
            val event = createBatteryEvent(id = "e1", deviceId = "d1")

            syncManager.applyRemoteUpdate(
                RemoteUpdate(
                    isFullSnapshot = false,
                    deviceTypes = listOf(type),
                    devices = listOf(device),
                    events = listOf(event),
                ),
            )

            assertEquals(1, local.devices.size)
            assertEquals(device, local.devices[0])
            assertEquals(1, local.deviceTypes.size)
            assertEquals(type, local.deviceTypes[0])
            assertEquals(1, local.events.size)
            assertEquals(event, local.events[0])
            repoScope.cancel()
        }

    @Test
    fun `applyRemoteUpdate applies deletions for incremental sync`() =
        runTest(testDispatcher) {
            val local = FakeLocalDataSource()
            local.addDevice(createDevice(id = "d1", name = "Old"))
            local.addDeviceType(DeviceType(id = "t1", name = "Old Type"))
            local.addEvent(createBatteryEvent(id = "e1", deviceId = "d1"))

            val (_, syncManager, repoScope) = createRepo(local)
            syncManager.applyRemoteUpdate(
                RemoteUpdate(
                    isFullSnapshot = false,
                    deviceTypes = emptyList(),
                    devices = emptyList(),
                    events = emptyList(),
                    deletedDeviceIds = listOf("d1"),
                    deletedDeviceTypeIds = listOf("t1"),
                    deletedEventIds = listOf("e1"),
                ),
            )

            assertTrue(local.deletedDeviceIds.contains("d1"))
            assertTrue(local.deletedDeviceTypeIds.contains("t1"))
            assertTrue(local.deletedEventIds.contains("e1"))
            repoScope.cancel()
        }

    @Test
    fun `applyRemoteUpdate does NOT apply deletions for full snapshot`() =
        runTest(testDispatcher) {
            val local = FakeLocalDataSource()
            local.addDevice(createDevice(id = "d1", name = "Keep Me"))
            val (_, syncManager, repoScope) = createRepo(local)

            syncManager.applyRemoteUpdate(
                RemoteUpdate(
                    isFullSnapshot = true,
                    deviceTypes = emptyList(),
                    devices = emptyList(),
                    events = emptyList(),
                    deletedDeviceIds = listOf("d1"),
                    deletedDeviceTypeIds = listOf("t1"),
                    deletedEventIds = listOf("e1"),
                ),
            )

            assertTrue(local.deletedDeviceIds.isEmpty())
            assertTrue(local.deletedDeviceTypeIds.isEmpty())
            assertTrue(local.deletedEventIds.isEmpty())
            repoScope.cancel()
        }

    @Test
    fun `applyRemoteUpdate handles empty update gracefully`() =
        runTest(testDispatcher) {
            val local = FakeLocalDataSource()
            val (_, syncManager, repoScope) = createRepo(local)

            syncManager.applyRemoteUpdate(
                RemoteUpdate(
                    isFullSnapshot = false,
                    deviceTypes = emptyList(),
                    devices = emptyList(),
                    events = emptyList(),
                ),
            )

            assertEquals(0, local.devices.size)
            assertEquals(0, local.deviceTypes.size)
            assertEquals(0, local.events.size)
            repoScope.cancel()
        }

    // ───────────────────────────────────────────────────────
    // Subscribe Behavior Tests
    // ───────────────────────────────────────────────────────

    @Test
    fun `subscribe stores received updates in local data source`() =
        runTest(testDispatcher) {
            val local = FakeLocalDataSource()
            val remote = ControllableRemoteDataSource()
            val device = createDevice(id = "d1", name = "Subscribed Device")
            val channel = Channel<RemoteUpdate>(Channel.UNLIMITED)
            remote.onSubscribe = { channel.receiveAsFlow() }

            val (_, _, repoScope) = createRepo(local, remote)
            testDispatcher.scheduler.advanceTimeBy(100)
            testDispatcher.scheduler.runCurrent()

            channel.trySend(
                RemoteUpdate(
                    isFullSnapshot = false,
                    devices = listOf(device),
                    deviceTypes = emptyList(),
                    events = emptyList(),
                ),
            )
            testDispatcher.scheduler.advanceTimeBy(100)
            testDispatcher.scheduler.runCurrent()

            assertEquals(1, local.devices.size)
            assertEquals(device, local.devices[0])
            repoScope.cancel()
        }

    @Test
    fun `subscribe sets syncStatus to Success on update`() =
        runTest(testDispatcher) {
            val local = FakeLocalDataSource()
            val remote = ControllableRemoteDataSource()
            val channel = Channel<RemoteUpdate>(Channel.UNLIMITED)
            remote.onSubscribe = { channel.receiveAsFlow() }

            val (repo, _, repoScope) = createRepo(local, remote)
            testDispatcher.scheduler.advanceTimeBy(100)
            testDispatcher.scheduler.runCurrent()

            channel.trySend(
                RemoteUpdate(
                    isFullSnapshot = false,
                    devices = emptyList(),
                    deviceTypes = emptyList(),
                    events = emptyList(),
                ),
            )
            testDispatcher.scheduler.advanceTimeBy(100)
            testDispatcher.scheduler.runCurrent()

            assertEquals(SyncStatus.Success, repo.syncStatus.value)
            repoScope.cancel()
        }

    @Test
    fun `subscribe processes multiple updates`() =
        runTest(testDispatcher) {
            val local = FakeLocalDataSource()
            val remote = ControllableRemoteDataSource()
            val channel = Channel<RemoteUpdate>(Channel.UNLIMITED)
            remote.onSubscribe = { channel.receiveAsFlow() }

            val (_, _, repoScope) = createRepo(local, remote)
            testDispatcher.scheduler.advanceTimeBy(100)
            testDispatcher.scheduler.runCurrent()

            repeat(3) { i ->
                channel.trySend(
                    RemoteUpdate(
                        isFullSnapshot = false,
                        devices = listOf(createDevice(id = "d$i", name = "Device $i")),
                        deviceTypes = emptyList(),
                        events = emptyList(),
                    ),
                )
                testDispatcher.scheduler.advanceTimeBy(100)
                testDispatcher.scheduler.runCurrent()
            }

            assertEquals(3, local.devices.size)
            repoScope.cancel()
        }

    @Test
    fun `subscribe failure sets syncStatus to Failed ConnectionFailed`() =
        runTest(testDispatcher) {
            val remote = ControllableRemoteDataSource()
            remote.onSubscribe = { throw RuntimeException("Connection refused") }

            val (repo, _, repoScope) = createRepo(remote = remote)
            testDispatcher.scheduler.advanceTimeBy(100)
            testDispatcher.scheduler.runCurrent()

            val status = repo.syncStatus.value
            assertIs<SyncStatus.Failed>(status)
            assertIs<DataError.Network.ConnectionFailed>(status.error)
            repoScope.cancel()
        }

    @Test
    fun `subscribe reconnects when stream ends normally`() =
        runTest(testDispatcher) {
            val remote = ControllableRemoteDataSource()
            remote.onSubscribe = { emptyFlow() }

            val (_, _, repoScope) = createRepo(remote = remote)
            // First subscribe attempt
            testDispatcher.scheduler.advanceTimeBy(100)
            testDispatcher.scheduler.runCurrent()
            val firstCount = remote.subscribeCallCount

            // After stream ends, wait past the 1s backoff for reconnect
            testDispatcher.scheduler.advanceTimeBy(1500)
            testDispatcher.scheduler.runCurrent()

            assertTrue(
                remote.subscribeCallCount > firstCount,
                "Expected reconnect, calls: ${remote.subscribeCallCount}",
            )
            repoScope.cancel()
        }

    // ───────────────────────────────────────────────────────
    // Backoff Tests
    // ───────────────────────────────────────────────────────

    @Test
    fun `nextBackoff doubles and caps correctly`() {
        assertEquals(2000L, DefaultSyncManager.nextBackoff(1000))
        assertEquals(4000L, DefaultSyncManager.nextBackoff(2000))
        assertEquals(16000L, DefaultSyncManager.nextBackoff(8000))
        // Cap at MAX_BACKOFF_MS
        assertEquals(
            DefaultSyncManager.MAX_BACKOFF_MS,
            DefaultSyncManager.nextBackoff(DefaultSyncManager.MAX_BACKOFF_MS),
        )
        assertEquals(
            DefaultSyncManager.MAX_BACKOFF_MS,
            DefaultSyncManager.nextBackoff(20_000),
        )
    }

    @Test
    fun `subscribe retries with increasing backoff on repeated failures`() =
        runTest(testDispatcher) {
            val remote = ControllableRemoteDataSource()
            remote.onSubscribe = { throw RuntimeException("fail") }

            val (_, _, repoScope) = createRepo(remote = remote)
            // First attempt
            testDispatcher.scheduler.advanceTimeBy(100)
            testDispatcher.scheduler.runCurrent()
            val c1 = remote.subscribeCallCount
            assertTrue(c1 >= 1, "First subscribe should happen, got $c1")

            // After first failure: 1s backoff. Advance 1.5s → should retry.
            testDispatcher.scheduler.advanceTimeBy(1500)
            testDispatcher.scheduler.runCurrent()
            val c2 = remote.subscribeCallCount
            assertTrue(c2 > c1, "Should retry after 1s backoff")

            // After second failure at t≈1000: 2s backoff → retry at t≈3000.
            // Current clock is t≈1600. Advance 1300ms → t≈2900, still before retry.
            testDispatcher.scheduler.advanceTimeBy(1300)
            testDispatcher.scheduler.runCurrent()
            assertEquals(c2, remote.subscribeCallCount, "Should not retry before 2s backoff")

            // Advance 200ms more → t≈3100, past the 2s backoff → should retry.
            testDispatcher.scheduler.advanceTimeBy(200)
            testDispatcher.scheduler.runCurrent()
            assertTrue(remote.subscribeCallCount > c2, "Should retry after 2s backoff")

            repoScope.cancel()
        }

    @Test
    fun `backoff resets after successful data receipt`() =
        runTest(testDispatcher) {
            val remote = ControllableRemoteDataSource()
            val emptyUpdate = RemoteUpdate(
                isFullSnapshot = false,
                devices = emptyList(),
                deviceTypes = emptyList(),
                events = emptyList(),
            )
            val results = mutableListOf<() -> Flow<RemoteUpdate>>(
                { throw RuntimeException("fail 1") },
                { throw RuntimeException("fail 2") },
                { flowOf(emptyUpdate) }, // success → resets backoff, then stream completes
                { throw RuntimeException("fail 3") },
                { flow { awaitCancellation() } },
            )
            var idx = 0
            remote.onSubscribe = {
                val factory = results.getOrElse(idx) { { flow<RemoteUpdate> { awaitCancellation() } } }
                idx++
                factory()
            }

            val (_, _, repoScope) = createRepo(remote = remote)
            // Call 1: fails → backoff 1s
            testDispatcher.scheduler.advanceTimeBy(100)
            testDispatcher.scheduler.runCurrent()

            // Call 2: after 1s backoff, fails → backoff 2s
            testDispatcher.scheduler.advanceTimeBy(1500)
            testDispatcher.scheduler.runCurrent()

            // Call 3: after 2s backoff, succeeds (emits data → resets backoff), stream completes
            testDispatcher.scheduler.advanceTimeBy(2500)
            testDispatcher.scheduler.runCurrent()

            // If backoff NOT reset: next backoff would be 4s (doubled from 2s).
            // If backoff IS reset: next backoff is 1s (INITIAL_BACKOFF_MS).
            // Wait 1.5s — enough for reset backoff (1s), not enough for non-reset (4s).
            testDispatcher.scheduler.advanceTimeBy(1500)
            testDispatcher.scheduler.runCurrent()

            assertTrue(idx >= 4, "Backoff should have reset to 1s, expected 4 calls, got $idx")
            repoScope.cancel()
        }

    // ───────────────────────────────────────────────────────
    // Helpers
    // ───────────────────────────────────────────────────────

    private fun createDevice(
        id: String,
        name: String,
        typeId: String = "type-1",
    ): Device =
        Device(
            id = id,
            name = name,
            typeId = typeId,
            batteryLastReplaced = Instant.DISTANT_PAST,
            lastUpdated = Instant.DISTANT_PAST,
        )

    private fun createBatteryEvent(
        id: String,
        deviceId: String,
    ): BatteryEvent =
        BatteryEvent(
            id = id,
            deviceId = deviceId,
            date = Instant.DISTANT_PAST,
        )
}

private data class RepoTestHarness(
    val repo: DefaultDeviceRepository,
    val syncManager: DefaultSyncManager,
    val scope: CoroutineScope,
)

/**
 * A [RemoteDataSource] where push() always throws an exception (not returning false).
 * Used to test the catch-all exception handling in pushUpdate.
 */
private class ThrowingRemoteDataSource : RemoteDataSource {
    override val state: StateFlow<RemoteDataSourceState> =
        MutableStateFlow(RemoteDataSourceState.NotStarted)

    override fun subscribe(): Flow<RemoteUpdate> = flow { awaitCancellation() }

    override suspend fun push(update: RemoteUpdate): Boolean = throw RuntimeException("Network error")
}

/**
 * A [RemoteDataSource] with controllable subscribe behavior.
 * Each test configures [onSubscribe] to return the desired flow.
 */
private class ControllableRemoteDataSource : RemoteDataSource {
    var onSubscribe: () -> Flow<RemoteUpdate> = { flow { awaitCancellation() } }
    val pushedUpdates = mutableListOf<RemoteUpdate>()
    var subscribeCallCount = 0
        private set

    override val state: StateFlow<RemoteDataSourceState> =
        MutableStateFlow(RemoteDataSourceState.NotStarted)

    override fun subscribe(): Flow<RemoteUpdate> {
        subscribeCallCount++
        return onSubscribe()
    }

    override suspend fun push(update: RemoteUpdate): Boolean {
        pushedUpdates.add(update)
        return true
    }
}
