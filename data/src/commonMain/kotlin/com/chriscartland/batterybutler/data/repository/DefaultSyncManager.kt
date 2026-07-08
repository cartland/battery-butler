package com.chriscartland.batterybutler.data.repository

import co.touchlab.kermit.Logger
import com.chriscartland.batterybutler.datalocal.LocalDataSource
import com.chriscartland.batterybutler.datanetwork.RemoteDataSource
import com.chriscartland.batterybutler.domain.model.BatteryEvent
import com.chriscartland.batterybutler.domain.model.DataError
import com.chriscartland.batterybutler.domain.model.DataMode
import com.chriscartland.batterybutler.domain.model.Device
import com.chriscartland.batterybutler.domain.model.DeviceType
import com.chriscartland.batterybutler.domain.model.SyncStatus
import com.chriscartland.batterybutler.domain.repository.DataModeRepository
import com.chriscartland.batterybutler.domain.repository.RemoteUpdate
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.TimeoutCancellationException
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.launch
import kotlinx.coroutines.withTimeout
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
     * Subscribes to remote updates with exponential backoff on failure.
     *
     * On each successful connection, resets the backoff delay.
     * On failure, waits with increasing delay: 1s -> 2s -> 4s -> 8s -> ... -> 30s max.
     */
    private suspend fun subscribeWithRetry() {
        var backoffMs = INITIAL_BACKOFF_MS
        while (true) {
            val currentMode = dataModeRepository.dataMode.first()
            if (currentMode is DataMode.None) {
                delay(5000)
                continue
            }

            try {
                _syncStatus.value = SyncStatus.Syncing
                remoteDataSource.subscribe().collect { update ->
                    backoffMs = INITIAL_BACKOFF_MS
                    _syncStatus.value = SyncStatus.Success

                    applyRemoteUpdate(update)
                }
                Logger.d(TAG) { "Subscribe stream ended, reconnecting..." }
            } catch (e: Exception) {
                if (e is CancellationException) throw e
                Logger.e(TAG, e) { "Subscribe failed, retrying in ${backoffMs}ms" }
                _syncStatus.value = SyncStatus.Failed(
                    DataError.Network.ConnectionFailed(
                        message = "Sync disconnected",
                        cause = e.message,
                    ),
                )
            }
            delay(backoffMs.milliseconds)
            backoffMs = nextBackoff(backoffMs)
        }
    }

    internal suspend fun applyRemoteUpdate(update: RemoteUpdate) {
        Logger.d(TAG) { "Received update: ${update.devices.size} devices" }

        if (!update.isFullSnapshot) {
            update.deletedDeviceTypeIds.forEach { localDataSource.deleteDeviceType(it) }
            update.deletedDeviceIds.forEach { localDataSource.deleteDevice(it) }
            update.deletedEventIds.forEach { localDataSource.deleteEvent(it) }
        }
        localDataSource.addDeviceTypes(update.deviceTypes)
        localDataSource.addDevices(update.devices)
        localDataSource.addEvents(update.events)
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
                _syncStatus.value = SyncStatus.Failed(
                    DataError.Unknown(e.message ?: "Unknown error", e.toString()),
                )
            }
        }
    }

    override fun dismissSyncStatus() {
        _syncStatus.value = SyncStatus.Idle
    }

    override suspend fun resync(timeout: Duration) {
        val currentMode = dataModeRepository.dataMode.first()
        if (currentMode is DataMode.None) return

        _syncStatus.value = SyncStatus.Syncing
        try {
            // .first() takes only the next emission then cancels the underlying subscription --
            // correct for both a one-shot REST poll (completes on its own) and a long-lived
            // gRPC server stream (would otherwise never complete). withTimeout bounds the wait
            // so a server with nothing new to push can't hang the caller (e.g. a pull-to-refresh
            // spinner) forever.
            val update = withTimeout(timeout) { remoteDataSource.subscribe().first() }
            applyRemoteUpdate(update)
            _syncStatus.value = SyncStatus.Success
        } catch (e: CancellationException) {
            if (e !is TimeoutCancellationException) throw e
            Logger.d(TAG) { "resync() timed out waiting for an update" }
            _syncStatus.value = SyncStatus.Failed(
                DataError.Network.ConnectionFailed(message = "Sync timed out", cause = null),
            )
        } catch (e: Exception) {
            Logger.e(TAG, e) { "resync() failed" }
            _syncStatus.value = SyncStatus.Failed(
                DataError.Unknown(e.message ?: "Unknown error", e.toString()),
            )
        }
    }

    internal companion object {
        const val TAG = "SyncManager"
        val INITIAL_BACKOFF_MS = 1.seconds.inWholeMilliseconds
        val MAX_BACKOFF_MS = 30.seconds.inWholeMilliseconds

        internal fun nextBackoff(currentMs: Long): Long = (currentMs * 2).coerceAtMost(MAX_BACKOFF_MS)
    }
}
