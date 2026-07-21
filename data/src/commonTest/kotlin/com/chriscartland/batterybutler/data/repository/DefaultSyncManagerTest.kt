package com.chriscartland.batterybutler.data.repository

import com.chriscartland.batterybutler.datalocal.LocalDataSource
import com.chriscartland.batterybutler.datanetwork.RemoteDataSource
import com.chriscartland.batterybutler.datanetwork.RemoteDataSourceState
import com.chriscartland.batterybutler.domain.model.DataMode
import com.chriscartland.batterybutler.domain.model.Device
import com.chriscartland.batterybutler.domain.repository.RemoteUpdate
import com.chriscartland.batterybutler.testcommon.FakeDataModeRepository
import com.chriscartland.batterybutler.testcommon.FakeDeviceImageCache
import com.chriscartland.batterybutler.testcommon.FakeDeviceImageDataSource
import com.chriscartland.batterybutler.testcommon.FakeLocalDataSource
import kotlinx.coroutines.CompletableDeferred
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.Job
import kotlinx.coroutines.awaitCancellation
import kotlinx.coroutines.cancel
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.flow
import kotlinx.coroutines.launch
import kotlinx.coroutines.test.StandardTestDispatcher
import kotlinx.coroutines.test.advanceUntilIdle
import kotlinx.coroutines.test.runTest
import kotlin.test.Test
import kotlin.test.assertEquals
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
