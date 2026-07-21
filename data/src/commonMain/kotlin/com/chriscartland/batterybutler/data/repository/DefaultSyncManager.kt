package com.chriscartland.batterybutler.data.repository

import co.touchlab.kermit.Logger
import com.chriscartland.batterybutler.datalocal.LocalDataSource
import com.chriscartland.batterybutler.datanetwork.RemoteDataSource
import com.chriscartland.batterybutler.datanetwork.RemoteSyncException
import com.chriscartland.batterybutler.domain.model.BatteryEvent
import com.chriscartland.batterybutler.domain.model.DataError
import com.chriscartland.batterybutler.domain.model.DataMode
import com.chriscartland.batterybutler.domain.model.Device
import com.chriscartland.batterybutler.domain.model.DeviceType
import com.chriscartland.batterybutler.domain.model.SyncOutcome
import com.chriscartland.batterybutler.domain.model.SyncStatus
import com.chriscartland.batterybutler.domain.repository.DataModeRepository
import com.chriscartland.batterybutler.domain.repository.LabsAuthRepository
import com.chriscartland.batterybutler.domain.repository.RemoteUpdate
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.CoroutineStart
import kotlinx.coroutines.NonCancellable
import kotlinx.coroutines.TimeoutCancellationException
import kotlinx.coroutines.coroutineScope
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import kotlinx.coroutines.withTimeout
import kotlinx.coroutines.withTimeoutOrNull
import me.tatarka.inject.annotations.Inject
import kotlin.coroutines.cancellation.CancellationException
import kotlin.time.Duration
import kotlin.time.Duration.Companion.milliseconds
import kotlin.time.Duration.Companion.seconds

@Inject
class DefaultSyncManager(
    private val localDataSource: LocalDataSource,
    private val remoteDataSource: RemoteDataSource,
    private val dataModeRepository: DataModeRepository,
    private val deviceImageSyncCoordinator: DeviceImageSyncCoordinator,
    private val labsAuthRepository: LabsAuthRepository,
    private val scope: CoroutineScope,
) : SyncManager {
    private val _syncStatus = MutableStateFlow<SyncStatus>(SyncStatus.Idle)
    override val syncStatus: StateFlow<SyncStatus> = _syncStatus.asStateFlow()

    init {
        scope.launch {
            subscribeWithRetry()
        }
    }

    /**
     * Drives sync with a **self-owned cadence**: for each distinct [DataMode], [syncLoopForMode]
     * runs until the mode changes (the watcher below cancels and restarts it).
     *
     * The cadence deliberately does NOT depend on `dataMode` re-emissions or on the remote flow
     * staying alive. The previous shape — `remoteDataSource.subscribe().collect { ... }` over
     * [DelegatingRemoteDataSource]'s `dataMode.flatMapLatest { ... }` — starved in production:
     * the inner Labs REST flow does one GET, emits once, and *completes*, but `flatMapLatest`
     * swallows that completion and just waits for the next `dataMode` emission. Pre-#1379,
     * spurious DataStore-write re-emissions of `dataMode` were accidentally driving the retry
     * cadence; once #1379 added `distinctUntilChanged`, those stopped — and after one successful
     * sync (or one failure the typed exceptions of #1380 don't turn into a loop iteration) the
     * collect hung forever: "Syncing..." on screen, zero requests for half an hour. See
     * `bb-sync-loop-starvation` in TODO.md.
     */
    private suspend fun subscribeWithRetry() {
        while (true) {
            val mode = dataModeRepository.dataMode.first()
            coroutineScope {
                // UNDISPATCHED: the first iteration (status transition + subscribe) runs
                // synchronously from this task — same startup ordering as the pre-restructure
                // loop, so a push queued at the same moment still lands its terminal status
                // *after* the loop's initial Syncing. (collectLatest would add dispatch hops.)
                val loop = launch(start = CoroutineStart.UNDISPATCHED) { syncLoopForMode(mode) }
                // Park until the mode actually changes, then cancel the loop and go around.
                dataModeRepository.dataMode.first { it != mode }
                loop.cancel()
            }
        }
    }

    /**
     * The sync cadence for one [mode], running until the mode changes (the caller's
     * [collectLatest] cancels this coroutine; the NonCancellable guard inside
     * [applyRemoteUpdate] keeps a mid-flight snapshot apply atomic across that cancellation).
     *
     * - [DataMode.None]: nothing to sync; parks until the next mode change.
     * - Labs (REST) modes are **request/response**: each iteration performs one fetch
     *   (`subscribe().first()` — the flow emits exactly once or throws), applies it, then sleeps
     *   [SYNC_POLL_INTERVAL_MS]. The poll is what gives the loop self-driven progress.
     * - Mock/gRPC modes are **streams**: collect until the stream ends (each emission applied as
     *   it arrives), then reconnect on the failure/poll cadence below.
     *
     * Failure cadence: exponential backoff 1s -> 2s -> ... -> 30s, reset by any successful
     * iteration. A clean iteration sleeps the poll interval instead.
     */
    private suspend fun syncLoopForMode(mode: DataMode) {
        if (mode is DataMode.None) return

        awaitLabsSessionGate(mode)
        var backoffMs = INITIAL_BACKOFF_MS
        while (true) {
            var failed = false
            try {
                _syncStatus.value = SyncStatus.Syncing
                if (mode.isRequestResponse) {
                    val update = remoteDataSource.subscribe().first()
                    _syncStatus.value = SyncStatus.Success
                    applyRemoteUpdate(update)
                } else {
                    remoteDataSource.subscribe().collect { update ->
                        backoffMs = INITIAL_BACKOFF_MS
                        _syncStatus.value = SyncStatus.Success
                        applyRemoteUpdate(update)
                    }
                    Logger.d(TAG) { "Subscribe stream ended, reconnecting..." }
                }
            } catch (e: Exception) {
                if (e is CancellationException) throw e
                failed = true
                Logger.e(TAG, e) { "Sync attempt failed, retrying in ${backoffMs}ms" }
                // A typed wire failure (auth-required / transient token / server error) surfaces
                // as its own status; nothing was applied locally. The loop stays alive either
                // way and keeps retrying with backoff.
                _syncStatus.value = when (e) {
                    is RemoteSyncException -> e.toSyncOutcome().toSyncStatus()

                    else -> SyncStatus.Failed(
                        DataError.Network.ConnectionFailed(
                            message = "Sync disconnected",
                            cause = e.message,
                        ),
                    )
                }
            }
            if (failed) {
                delay(backoffMs.milliseconds)
                backoffMs = nextBackoff(backoffMs)
            } else {
                backoffMs = INITIAL_BACKOFF_MS
                delay(SYNC_POLL_INTERVAL_MS.milliseconds)
            }
        }
    }

    /** Request/response modes fetch one snapshot per call; stream modes stay subscribed. */
    private val DataMode.isRequestResponse: Boolean
        get() = this is DataMode.LabsStaging || this is DataMode.LabsProd

    /**
     * Cold-start gate: in a Labs mode, wait for the environment's persisted-session restore to
     * resolve before firing any Labs request — otherwise the first loop iterations race the
     * restore, get a null-session token result, and churn a spurious
     * [SyncStatus.AuthRequired]([com.chriscartland.batterybutler.domain.model.SyncAuthReason
     * .NO_SESSION]) on every believed-signed-in launch. Resolution is guaranteed (a transient
     * restore failure resolves as TRANSIENT_FAILURE rather than blocking), and the extra
     * [withTimeoutOrNull] is a belt-and-braces bound so a wedged auth layer can only ever delay
     * sync, never stop it. Non-Labs modes (None/Mock/gRPC) are unaffected.
     */
    private suspend fun awaitLabsSessionGate(mode: DataMode) {
        if (mode !is DataMode.LabsStaging && mode !is DataMode.LabsProd) return
        val resolution = withTimeoutOrNull(SESSION_RESTORE_GATE_TIMEOUT_MS) {
            labsAuthRepository.awaitLabsSessionRestore()
        }
        Logger.d(TAG) { "Labs session restore gate resolved: $resolution" }
    }

    internal suspend fun applyRemoteUpdate(update: RemoteUpdate) {
        Logger.d(TAG) { "Received update: ${update.devices.size} devices" }

        // The local-DB writes run under NonCancellable so a mode re-emission that cancels the
        // collecting subscribe() (DelegatingRemoteDataSource does `dataMode.flatMapLatest { … }`)
        // can never tear a snapshot apply half-committed — e.g. after sign-out cleared the DB, a
        // cancellation between the delete pass and the insert pass would leave the device list
        // empty even though a full snapshot arrived. Scoped to the local writes only; the image
        // coordinator below stays cancellable (it does its own network work). See
        // `bb-signin-empty-list` in TODO.md.
        withContext(NonCancellable) {
            if (!update.isFullSnapshot) {
                update.deletedDeviceTypeIds.forEach { localDataSource.deleteDeviceType(it) }
                update.deletedDeviceIds.forEach { localDataSource.deleteDevice(it) }
                update.deletedEventIds.forEach { localDataSource.deleteEvent(it) }
            }
            localDataSource.addDeviceTypes(update.deviceTypes)
            localDataSource.addDevices(update.devices)
            localDataSource.addEvents(update.events)
        }

        deviceImageSyncCoordinator.onRemoteUpdate(update)
    }

    override fun pushUpdate(
        deviceTypes: List<DeviceType>,
        devices: List<Device>,
        events: List<BatteryEvent>,
        deletedDeviceTypeIds: List<String>,
        deletedDeviceIds: List<String>,
        deletedEventIds: List<String>,
    ) {
        scope.launch {
            val currentMode = dataModeRepository.dataMode.first()
            if (currentMode is DataMode.None) {
                Logger.d(TAG) { "Data Mode None: Skipping push update" }
                return@launch
            }

            awaitLabsSessionGate(currentMode)

            _syncStatus.value = SyncStatus.Syncing
            try {
                val success = remoteDataSource.push(
                    RemoteUpdate(
                        isFullSnapshot = false,
                        deviceTypes = deviceTypes,
                        devices = devices,
                        events = events,
                        deletedDeviceTypeIds = deletedDeviceTypeIds,
                        deletedDeviceIds = deletedDeviceIds,
                        deletedEventIds = deletedEventIds,
                    ),
                )
                if (success) {
                    _syncStatus.value = SyncStatus.Success
                } else {
                    _syncStatus.value = SyncStatus.Failed(
                        DataError.Network.PushFailed("Server rejected sync request"),
                    )
                }
            } catch (e: Exception) {
                if (e is CancellationException) throw e
                Logger.e(TAG, e) { "Push failed" }
                _syncStatus.value = when (e) {
                    is RemoteSyncException -> e.toSyncOutcome().toSyncStatus()

                    else -> SyncStatus.Failed(
                        DataError.Unknown(e.message ?: "Unknown error", e.toString()),
                    )
                }
            }
        }
    }

    override fun dismissSyncStatus() {
        _syncStatus.value = SyncStatus.Idle
    }

    override suspend fun resync(timeout: Duration): SyncOutcome {
        val currentMode = dataModeRepository.dataMode.first()
        if (currentMode is DataMode.None) return SyncOutcome.Skipped

        awaitLabsSessionGate(currentMode)

        _syncStatus.value = SyncStatus.Syncing
        val outcome = try {
            // .first() takes only the next emission then cancels the underlying subscription --
            // correct for both a one-shot REST poll (completes on its own) and a long-lived
            // gRPC server stream (would otherwise never complete). withTimeout bounds the wait
            // so a server with nothing new to push can't hang the caller (e.g. a pull-to-refresh
            // spinner) forever.
            val update = withTimeout(timeout) { remoteDataSource.subscribe().first() }
            applyRemoteUpdate(update)
            SyncOutcome.Success
        } catch (e: CancellationException) {
            if (e !is TimeoutCancellationException) throw e
            Logger.d(TAG) { "resync() timed out waiting for an update" }
            SyncOutcome.Failed(
                DataError.Network.ConnectionFailed(message = "Sync timed out", cause = null),
            )
        } catch (e: RemoteSyncException) {
            // Typed wire failure: auth-required (401 / no session) or a server error. Nothing
            // was applied locally; the caller gets the real outcome instead of a fake success.
            Logger.e(TAG, e) { "resync() failed at the wire layer" }
            e.toSyncOutcome()
        } catch (e: Exception) {
            Logger.e(TAG, e) { "resync() failed" }
            SyncOutcome.Failed(
                DataError.Unknown(e.message ?: "Unknown error", e.toString()),
            )
        }
        _syncStatus.value = outcome.toSyncStatus()
        return outcome
    }

    internal companion object {
        const val TAG = "SyncManager"
        val INITIAL_BACKOFF_MS = 1.seconds.inWholeMilliseconds
        val MAX_BACKOFF_MS = 30.seconds.inWholeMilliseconds

        /** Upper bound on how long the cold-start Labs session gate may delay a sync attempt. */
        val SESSION_RESTORE_GATE_TIMEOUT_MS = 30.seconds.inWholeMilliseconds

        /**
         * Foreground poll cadence for request/response (Labs REST) modes after a clean
         * iteration. The loop must self-drive its next fetch — nothing else re-triggers it (see
         * `bb-sync-loop-starvation`); pull-to-refresh / post-edit pushes / sign-in resync cover
         * immediacy, so a minute-scale poll is plenty for a battery tracker.
         */
        val SYNC_POLL_INTERVAL_MS = 60.seconds.inWholeMilliseconds

        internal fun nextBackoff(currentMs: Long): Long = (currentMs * 2).coerceAtMost(MAX_BACKOFF_MS)
    }
}

/** Maps a typed wire failure to the [SyncOutcome] it means for the attempt that threw it. */
private fun RemoteSyncException.toSyncOutcome(): SyncOutcome =
    when (this) {
        is RemoteSyncException.AuthRequired -> SyncOutcome.AuthRequired(reason)

        is RemoteSyncException.ServerError -> SyncOutcome.Failed(
            DataError.Network.ServerError(cause = "HTTP $statusCode"),
        )

        // A session exists but its token couldn't be refreshed for transient (network) reasons.
        // Deliberately the network-failure path, never AuthRequired: a flaky connection must not
        // show "sign in required" to a signed-in user.
        is RemoteSyncException.TokenUnavailable -> SyncOutcome.Failed(
            DataError.Network.ConnectionFailed(
                message = "Sync session refresh failed",
                cause = message,
            ),
        )
    }

/**
 * The [SyncStatus] to publish for a terminal [SyncOutcome]. [SyncOutcome.Skipped] maps to
 * [SyncStatus.Idle] for exhaustiveness only — sync-skipping paths return before publishing.
 */
private fun SyncOutcome.toSyncStatus(): SyncStatus =
    when (this) {
        SyncOutcome.Success -> SyncStatus.Success
        SyncOutcome.Skipped -> SyncStatus.Idle
        is SyncOutcome.AuthRequired -> SyncStatus.AuthRequired(reason)
        is SyncOutcome.Failed -> SyncStatus.Failed(error)
    }
