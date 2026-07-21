package com.chriscartland.batterybutler.data.repository

import com.chriscartland.batterybutler.datalocal.LocalDataSource
import com.chriscartland.batterybutler.datanetwork.RemoteDataSource
import com.chriscartland.batterybutler.datanetwork.RemoteDataSourceState
import com.chriscartland.batterybutler.datanetwork.RemoteSyncException
import com.chriscartland.batterybutler.domain.model.DataError
import com.chriscartland.batterybutler.domain.model.DataMode
import com.chriscartland.batterybutler.domain.model.Device
import com.chriscartland.batterybutler.domain.model.LabsSessionRestoreResult
import com.chriscartland.batterybutler.domain.model.SyncAuthReason
import com.chriscartland.batterybutler.domain.model.SyncOutcome
import com.chriscartland.batterybutler.domain.model.SyncStatus
import com.chriscartland.batterybutler.domain.repository.RemoteUpdate
import com.chriscartland.batterybutler.testcommon.FakeDataModeRepository
import com.chriscartland.batterybutler.testcommon.FakeDeviceImageCache
import com.chriscartland.batterybutler.testcommon.FakeDeviceImageDataSource
import com.chriscartland.batterybutler.testcommon.FakeLabsAuthRepository
import com.chriscartland.batterybutler.testcommon.FakeLocalDataSource
import kotlinx.coroutines.CompletableDeferred
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.Job
import kotlinx.coroutines.async
import kotlinx.coroutines.awaitCancellation
import kotlinx.coroutines.cancel
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.flow
import kotlinx.coroutines.flow.flowOf
import kotlinx.coroutines.launch
import kotlinx.coroutines.test.StandardTestDispatcher
import kotlinx.coroutines.test.advanceTimeBy
import kotlinx.coroutines.test.advanceUntilIdle
import kotlinx.coroutines.test.runCurrent
import kotlinx.coroutines.test.runTest
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertIs
import kotlin.test.assertTrue
import kotlin.time.Duration.Companion.seconds
import kotlin.time.ExperimentalTime
import kotlin.time.Instant

@OptIn(ExperimentalCoroutinesApi::class, ExperimentalTime::class)
class DefaultSyncManagerTest {
    private val testDispatcher = StandardTestDispatcher()

    /**
     * A full-snapshot `applyRemoteUpdate` must commit atomically even when the coroutine collecting
     * it is cancelled mid-write.
     *
     * This is the exact failure that emptied the device list after a sign-out → sign-in: sign-out
     * clears the local DB, then the repopulating `/sync` is collected inside
     * `DelegatingRemoteDataSource.subscribe()`'s `dataMode.flatMapLatest { … }`. A spurious
     * `dataMode` re-emission cancels that collector; if the snapshot write were cancellable it would
     * be torn between the (skipped, for a full snapshot) delete pass and the insert pass, leaving the
     * list empty despite a 200 that returned every device. The `withContext(NonCancellable)` guard in
     * [DefaultSyncManager.applyRemoteUpdate] closes that. See `bb-signin-empty-list` in TODO.md.
     */
    @Test
    fun `full snapshot commits even when the collecting coroutine is cancelled mid-write`() =
        runTest(testDispatcher) {
            val gate = CompletableDeferred<Unit>()
            val fakeLocal = FakeLocalDataSource()
            val scope = CoroutineScope(testDispatcher + Job())
            val syncManager = DefaultSyncManager(
                localDataSource = GatedLocalDataSource(fakeLocal, gate),
                remoteDataSource = ParkedRemoteDataSource(),
                dataModeRepository = FakeDataModeRepository(DataMode.Mock),
                deviceImageSyncCoordinator = DeviceImageSyncCoordinator(
                    FakeDeviceImageDataSource(),
                    FakeDeviceImageCache(),
                    scope,
                ),
                labsAuthRepository = FakeLabsAuthRepository(),
                scope = scope,
            )

            val devices = (1..52).map { createDevice(id = "d$it", name = "Device $it") }
            val snapshot = RemoteUpdate(
                isFullSnapshot = true,
                deviceTypes = emptyList(),
                devices = devices,
                events = emptyList(),
            )

            // Stand in for the flatMapLatest inner collector that runs applyRemoteUpdate.
            val applyJob = launch { syncManager.applyRemoteUpdate(snapshot) }
            advanceUntilIdle() // addDeviceTypes runs; addDevices suspends on the gate

            // A spurious dataMode re-emission cancels the collector while the write is in flight.
            applyJob.cancel()
            advanceUntilIdle()
            assertEquals(0, fakeLocal.devices.size, "write is still gated, nothing committed yet")

            // Releasing the gate: under NonCancellable the write finishes despite the cancel.
            gate.complete(Unit)
            advanceUntilIdle()

            assertEquals(
                52,
                fakeLocal.devices.size,
                "the full snapshot must land even though the collector was cancelled mid-write",
            )
            scope.cancel()
        }

    /**
     * An auth failure from the wire layer must become a visible [SyncStatus.AuthRequired],
     * must apply nothing locally, and must NOT kill the background loop — it keeps retrying
     * with its normal backoff (PR B reacts to the auth state; this PR only makes it visible
     * and inert).
     */
    @Test
    fun `an auth failure from subscribe sets AuthRequired applies nothing and keeps the loop retrying`() =
        runTest(testDispatcher) {
            val fakeLocal = FakeLocalDataSource()
            val scope = CoroutineScope(testDispatcher + Job())
            val remote = ScriptedRemoteDataSource().apply {
                onSubscribe = { flow { throw RemoteSyncException.AuthRequired(SyncAuthReason.TOKEN_EXPIRED) } }
            }
            val syncManager = createSyncManager(fakeLocal, remote, scope)

            runCurrent() // first subscribe attempt fails with the typed auth error
            assertEquals(SyncStatus.AuthRequired(SyncAuthReason.TOKEN_EXPIRED), syncManager.syncStatus.value)
            assertEquals(1, remote.subscribeCount)
            assertEquals(0, fakeLocal.devices.size, "an auth failure must never touch local data")

            advanceTimeBy(DefaultSyncManager.INITIAL_BACKOFF_MS + 1)
            runCurrent() // loop survived: a second attempt fired after the backoff delay
            assertTrue(remote.subscribeCount >= 2, "the loop must keep retrying, got ${remote.subscribeCount} attempts")
            assertEquals(0, fakeLocal.devices.size)
            scope.cancel()
        }

    @Test
    fun `a server error from subscribe sets Failed with the status retained and keeps the loop retrying`() =
        runTest(testDispatcher) {
            val fakeLocal = FakeLocalDataSource()
            val scope = CoroutineScope(testDispatcher + Job())
            val remote = ScriptedRemoteDataSource().apply {
                onSubscribe = { flow { throw RemoteSyncException.ServerError(statusCode = 500) } }
            }
            val syncManager = createSyncManager(fakeLocal, remote, scope)

            runCurrent()
            val status = syncManager.syncStatus.value
            assertIs<SyncStatus.Failed>(status)
            val error = status.error
            assertIs<DataError.Network.ServerError>(error)
            assertEquals("HTTP 500", error.cause)

            advanceTimeBy(DefaultSyncManager.INITIAL_BACKOFF_MS + 1)
            runCurrent()
            assertTrue(remote.subscribeCount >= 2, "the loop must survive a server error")
            assertEquals(0, fakeLocal.devices.size)
            scope.cancel()
        }

    @Test
    fun `resync surfaces an auth failure as a typed outcome and applies nothing locally`() =
        runTest(testDispatcher) {
            val fakeLocal = FakeLocalDataSource()
            val scope = CoroutineScope(testDispatcher + Job())
            val remote = ScriptedRemoteDataSource().apply {
                onSubscribe = { flow { throw RemoteSyncException.AuthRequired(SyncAuthReason.NO_SESSION) } }
            }
            val syncManager = createSyncManager(fakeLocal, remote, scope)

            val outcome = syncManager.resync(timeout = 5.seconds)

            assertEquals(SyncOutcome.AuthRequired(SyncAuthReason.NO_SESSION), outcome)
            assertEquals(SyncStatus.AuthRequired(SyncAuthReason.NO_SESSION), syncManager.syncStatus.value)
            assertEquals(0, fakeLocal.devices.size, "a failed resync must never touch local data")
            scope.cancel()
        }

    @Test
    fun `resync surfaces a server error as a typed Failed outcome`() =
        runTest(testDispatcher) {
            val fakeLocal = FakeLocalDataSource()
            val scope = CoroutineScope(testDispatcher + Job())
            val remote = ScriptedRemoteDataSource().apply {
                onSubscribe = { flow { throw RemoteSyncException.ServerError(statusCode = 503) } }
            }
            val syncManager = createSyncManager(fakeLocal, remote, scope)

            val outcome = syncManager.resync(timeout = 5.seconds)

            assertIs<SyncOutcome.Failed>(outcome)
            val error = outcome.error
            assertIs<DataError.Network.ServerError>(error)
            assertEquals("HTTP 503", error.cause)
            assertEquals(0, fakeLocal.devices.size)
            scope.cancel()
        }

    @Test
    fun `resync returns Success after applying the fetched snapshot`() =
        runTest(testDispatcher) {
            val fakeLocal = FakeLocalDataSource()
            val scope = CoroutineScope(testDispatcher + Job())
            val remote = ScriptedRemoteDataSource().apply {
                onSubscribe = {
                    flowOf(
                        RemoteUpdate(
                            isFullSnapshot = true,
                            deviceTypes = emptyList(),
                            devices = listOf(createDevice(id = "d1", name = "Device 1")),
                            events = emptyList(),
                        ),
                    )
                }
            }
            val syncManager = createSyncManager(fakeLocal, remote, scope)

            val outcome = syncManager.resync(timeout = 5.seconds)

            assertEquals(SyncOutcome.Success, outcome)
            assertEquals(SyncStatus.Success, syncManager.syncStatus.value)
            assertEquals(1, fakeLocal.devices.size)
            scope.cancel()
        }

    @Test
    fun `resync returns Skipped when no data mode is configured`() =
        runTest(testDispatcher) {
            val fakeLocal = FakeLocalDataSource()
            val scope = CoroutineScope(testDispatcher + Job())
            val remote = ScriptedRemoteDataSource()
            val syncManager = DefaultSyncManager(
                localDataSource = fakeLocal,
                remoteDataSource = remote,
                dataModeRepository = FakeDataModeRepository(DataMode.None),
                deviceImageSyncCoordinator = DeviceImageSyncCoordinator(
                    FakeDeviceImageDataSource(),
                    FakeDeviceImageCache(),
                    scope,
                ),
                labsAuthRepository = FakeLabsAuthRepository(),
                scope = scope,
            )

            val outcome = syncManager.resync(timeout = 5.seconds)

            assertEquals(SyncOutcome.Skipped, outcome)
            assertEquals(SyncStatus.Idle, syncManager.syncStatus.value)
            scope.cancel()
        }

    @Test
    fun `pushUpdate maps an auth failure to AuthRequired status`() =
        runTest(testDispatcher) {
            val fakeLocal = FakeLocalDataSource()
            val scope = CoroutineScope(testDispatcher + Job())
            val remote = ScriptedRemoteDataSource().apply {
                onPush = { throw RemoteSyncException.AuthRequired(SyncAuthReason.TOKEN_INVALID) }
            }
            val syncManager = createSyncManager(fakeLocal, remote, scope)

            syncManager.pushUpdate(devices = listOf(createDevice(id = "d1", name = "Device 1")))
            advanceUntilIdle()

            assertEquals(SyncStatus.AuthRequired(SyncAuthReason.TOKEN_INVALID), syncManager.syncStatus.value)
            scope.cancel()
        }

    // region Sync-loop starvation (self-driven cadence)

    /**
     * THE production starvation (`bb-sync-loop-starvation`): the remote flow emits one snapshot
     * and then never completes — exactly how `DelegatingRemoteDataSource`'s old
     * `dataMode.flatMapLatest { ... }` behaved after the inner one-shot REST fetch completed
     * (the completion is swallowed; post-#1379 no `dataMode` re-emission ever arrives). The loop
     * must have SELF-DRIVEN progress: with no preference writes and no mode changes, another
     * fetch must happen within the poll interval. Against origin/main this test FAILS —
     * `subscribeCount` stays 1 forever (one successful sync at 15:24, then zero requests for
     * 30+ minutes on the user's device).
     */
    @Test
    fun `after a successful Labs sync the loop fetches again within the poll interval`() =
        runTest(testDispatcher) {
            val fakeLocal = FakeLocalDataSource()
            val scope = CoroutineScope(testDispatcher + Job())
            val snapshot = RemoteUpdate(
                isFullSnapshot = true,
                deviceTypes = emptyList(),
                devices = listOf(createDevice(id = "d1", name = "Device 1")),
                events = emptyList(),
            )
            val remote = ScriptedRemoteDataSource().apply {
                // Emits once, then stays open forever — the flatMapLatest-swallowed-completion shape.
                onSubscribe = {
                    flow {
                        emit(snapshot)
                        awaitCancellation()
                    }
                }
            }
            createSyncManager(
                fakeLocal,
                remote,
                scope,
                dataMode = DataMode.LabsStaging(url = "https://staging.example"),
            )

            runCurrent()
            assertEquals(1, remote.subscribeCount)
            assertEquals(1, fakeLocal.devices.size, "the first fetch applied")

            advanceTimeBy(DefaultSyncManager.SYNC_POLL_INTERVAL_MS + 1)
            runCurrent()
            assertTrue(
                remote.subscribeCount >= 2,
                "the loop must self-drive its next fetch within the poll interval " +
                    "(got ${remote.subscribeCount} subscribes — the starved loop never fetches again)",
            )
            scope.cancel()
        }

    /** A mode change interrupts the poll delay: switching to None stops fetching immediately. */
    @Test
    fun `switching to None mid-poll stops the loop and switching back resumes it`() =
        runTest(testDispatcher) {
            val fakeLocal = FakeLocalDataSource()
            val scope = CoroutineScope(testDispatcher + Job())
            val remote = ScriptedRemoteDataSource().apply {
                onSubscribe = {
                    flow {
                        emit(
                            RemoteUpdate(
                                isFullSnapshot = true,
                                deviceTypes = emptyList(),
                                devices = emptyList(),
                                events = emptyList(),
                            ),
                        )
                        awaitCancellation()
                    }
                }
            }
            val dataModeRepository = FakeDataModeRepository(DataMode.LabsStaging(url = "https://staging.example"))
            DefaultSyncManager(
                localDataSource = fakeLocal,
                remoteDataSource = remote,
                dataModeRepository = dataModeRepository,
                deviceImageSyncCoordinator = DeviceImageSyncCoordinator(
                    FakeDeviceImageDataSource(),
                    FakeDeviceImageCache(),
                    scope,
                ),
                labsAuthRepository = FakeLabsAuthRepository(),
                scope = scope,
            )

            runCurrent()
            assertEquals(1, remote.subscribeCount)

            dataModeRepository.setDataMode(DataMode.None)
            runCurrent()
            advanceTimeBy(DefaultSyncManager.SYNC_POLL_INTERVAL_MS * 10)
            runCurrent()
            assertEquals(1, remote.subscribeCount, "None mode must fetch nothing")

            dataModeRepository.setDataMode(DataMode.LabsStaging(url = "https://staging.example"))
            runCurrent()
            assertEquals(2, remote.subscribeCount, "switching back must resume fetching immediately")
            scope.cancel()
        }

    // endregion

    // region Cold-start Labs session gate (PR B)

    /**
     * In a Labs mode, the background loop must fire ZERO remote requests until the environment's
     * session restore has resolved — pre-fix, the loop's first iterations raced the restore and
     * churned AuthRequired(NO_SESSION) on every believed-signed-in cold start. Against the
     * pre-fix code (no gate) this test fails at the first assertion: subscribeCount is already 1.
     */
    @Test
    fun `sync fires no request in a Labs mode until the session restore resolves`() =
        runTest(testDispatcher) {
            val fakeLocal = FakeLocalDataSource()
            val scope = CoroutineScope(testDispatcher + Job())
            val remote = ScriptedRemoteDataSource()
            val labsAuth = FakeLabsAuthRepository().apply {
                sessionRestoreGate = CompletableDeferred()
                sessionRestoreResult = LabsSessionRestoreResult.RESTORED
            }
            createSyncManager(
                fakeLocal,
                remote,
                scope,
                dataMode = DataMode.LabsStaging(url = "https://staging.example"),
                labsAuthRepository = labsAuth,
            )

            runCurrent()
            assertEquals(0, remote.subscribeCount, "no request may fire while the restore is unresolved")
            assertEquals(1, labsAuth.sessionRestoreAwaitCount, "the loop must be waiting on the gate")

            labsAuth.sessionRestoreGate?.complete(Unit)
            runCurrent()
            assertTrue(remote.subscribeCount >= 1, "sync must proceed once the restore resolves")
            scope.cancel()
        }

    /**
     * A *transiently*-failed restore resolves (as TRANSIENT_FAILURE) instead of blocking: sync
     * proceeds and surfaces whatever the wire yields — here a transient token failure, which maps
     * to the NETWORK failure path, never AuthRequired ("flaky network must not say sign in").
     */
    @Test
    fun `sync proceeds after a transiently-failed restore and a transient token failure maps to a network status`() =
        runTest(testDispatcher) {
            val fakeLocal = FakeLocalDataSource()
            val scope = CoroutineScope(testDispatcher + Job())
            val remote = ScriptedRemoteDataSource().apply {
                onSubscribe = { flow { throw RemoteSyncException.TokenUnavailable() } }
            }
            val labsAuth = FakeLabsAuthRepository().apply {
                sessionRestoreResult = LabsSessionRestoreResult.TRANSIENT_FAILURE
            }
            val syncManager = createSyncManager(
                fakeLocal,
                remote,
                scope,
                dataMode = DataMode.LabsStaging(url = "https://staging.example"),
                labsAuthRepository = labsAuth,
            )

            runCurrent()
            assertTrue(remote.subscribeCount >= 1, "a transient restore failure must not block sync")
            val status = syncManager.syncStatus.value
            assertIs<SyncStatus.Failed>(status, "a transient token failure is a network problem, got $status")
            assertIs<DataError.Network.ConnectionFailed>(status.error)
            scope.cancel()
        }

    /** The gate is Labs-only: None/Mock/gRPC modes never consult the Labs auth repository. */
    @Test
    fun `non-Labs modes never await the session gate`() =
        runTest(testDispatcher) {
            val fakeLocal = FakeLocalDataSource()
            val scope = CoroutineScope(testDispatcher + Job())
            val labsAuth = FakeLabsAuthRepository().apply { sessionRestoreGate = CompletableDeferred() }
            createSyncManager(
                fakeLocal,
                ScriptedRemoteDataSource(),
                scope,
                dataMode = DataMode.Mock,
                labsAuthRepository = labsAuth,
            )

            runCurrent()
            assertEquals(0, labsAuth.sessionRestoreAwaitCount, "Mock mode must not touch the Labs gate")
            scope.cancel()
        }

    /** resync() honors the same gate: no request until the restore resolves. */
    @Test
    fun `resync awaits the session restore gate in a Labs mode`() =
        runTest(testDispatcher) {
            val fakeLocal = FakeLocalDataSource()
            val scope = CoroutineScope(testDispatcher + Job())
            val remote = ScriptedRemoteDataSource().apply {
                onSubscribe = {
                    flowOf(
                        RemoteUpdate(
                            isFullSnapshot = true,
                            deviceTypes = emptyList(),
                            devices = listOf(createDevice(id = "d1", name = "Device 1")),
                            events = emptyList(),
                        ),
                    )
                }
            }
            val labsAuth = FakeLabsAuthRepository().apply {
                sessionRestoreGate = CompletableDeferred()
                sessionRestoreResult = LabsSessionRestoreResult.RESTORED
            }
            val syncManager = createSyncManager(
                fakeLocal,
                remote,
                scope,
                dataMode = DataMode.LabsStaging(url = "https://staging.example"),
                labsAuthRepository = labsAuth,
            )

            val resync = async { syncManager.resync(timeout = 5.seconds) }
            runCurrent()
            assertTrue(!resync.isCompleted, "resync must be parked on the gate")

            labsAuth.sessionRestoreGate?.complete(Unit)
            runCurrent()
            assertEquals(SyncOutcome.Success, resync.await())
            scope.cancel()
        }

    /**
     * Reactive session loss keeps local data: a terminal AuthRequired from the wire (the
     * retry-once policy exhausted) surfaces as a status — and the local DB rows survive
     * untouched. Only the explicit sign-out use case clears local data.
     */
    @Test
    fun `a terminal auth failure leaves existing local rows untouched`() =
        runTest(testDispatcher) {
            val fakeLocal = FakeLocalDataSource()
            fakeLocal.addDevices(listOf(createDevice(id = "d1", name = "Kept 1"), createDevice(id = "d2", name = "Kept 2")))
            val scope = CoroutineScope(testDispatcher + Job())
            val remote = ScriptedRemoteDataSource().apply {
                onSubscribe = { flow { throw RemoteSyncException.AuthRequired(SyncAuthReason.TOKEN_INVALID) } }
            }
            val syncManager = createSyncManager(
                fakeLocal,
                remote,
                scope,
                dataMode = DataMode.LabsStaging(url = "https://staging.example"),
                labsAuthRepository = FakeLabsAuthRepository(),
            )

            runCurrent()
            assertEquals(SyncStatus.AuthRequired(SyncAuthReason.TOKEN_INVALID), syncManager.syncStatus.value)
            assertEquals(2, fakeLocal.devices.size, "session loss must never clear the local device rows")
            scope.cancel()
        }

    // endregion

    private fun createSyncManager(
        fakeLocal: FakeLocalDataSource,
        remote: RemoteDataSource,
        scope: CoroutineScope,
        dataMode: DataMode = DataMode.Mock,
        labsAuthRepository: FakeLabsAuthRepository = FakeLabsAuthRepository(),
    ): DefaultSyncManager =
        DefaultSyncManager(
            localDataSource = fakeLocal,
            remoteDataSource = remote,
            dataModeRepository = FakeDataModeRepository(dataMode),
            deviceImageSyncCoordinator = DeviceImageSyncCoordinator(
                FakeDeviceImageDataSource(),
                FakeDeviceImageCache(),
                scope,
            ),
            labsAuthRepository = labsAuthRepository,
            scope = scope,
        )

    private fun createDevice(
        id: String,
        name: String,
    ): Device =
        Device(
            id = id,
            name = name,
            typeId = "type-1",
            batteryLastReplaced = Instant.DISTANT_PAST,
            lastUpdated = Instant.DISTANT_PAST,
        )
}

/**
 * A [RemoteDataSource] whose subscribe/push behavior a test scripts per case. The default
 * subscribe parks forever (so the manager's background loop stays quiet) and the default push
 * succeeds.
 */
private class ScriptedRemoteDataSource : RemoteDataSource {
    var subscribeCount = 0
    var onSubscribe: () -> Flow<RemoteUpdate> = { flow { awaitCancellation() } }
    var onPush: suspend (RemoteUpdate) -> Boolean = { true }

    override val state: StateFlow<RemoteDataSourceState> =
        MutableStateFlow(RemoteDataSourceState.Subscribed)

    override fun subscribe(): Flow<RemoteUpdate> {
        subscribeCount++
        return onSubscribe()
    }

    override suspend fun push(update: RemoteUpdate): Boolean = onPush(update)
}

/**
 * Delegates to a [FakeLocalDataSource] but makes `addDevices` block on [gate], so a test can hold
 * the snapshot write open and cancel the surrounding coroutine while it is suspended.
 */
private class GatedLocalDataSource(
    private val delegate: FakeLocalDataSource,
    private val gate: CompletableDeferred<Unit>,
) : LocalDataSource by delegate {
    override suspend fun addDevices(devices: List<Device>) {
        gate.await()
        delegate.addDevices(devices)
    }
}

/** A [RemoteDataSource] whose subscribe never emits, so the manager's retry loop just parks. */
private class ParkedRemoteDataSource : RemoteDataSource {
    override val state: StateFlow<RemoteDataSourceState> =
        MutableStateFlow(RemoteDataSourceState.NotStarted)

    override fun subscribe(): Flow<RemoteUpdate> = flow { awaitCancellation() }

    override suspend fun push(update: RemoteUpdate): Boolean = true
}
