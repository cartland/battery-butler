package com.chriscartland.batterybutler.domain.repository

import com.chriscartland.batterybutler.domain.model.BatteryEvent
import com.chriscartland.batterybutler.domain.model.DataError
import com.chriscartland.batterybutler.domain.model.Device
import com.chriscartland.batterybutler.domain.model.DeviceType
import com.chriscartland.batterybutler.domain.model.Result
import com.chriscartland.batterybutler.domain.model.SyncOutcome
import com.chriscartland.batterybutler.domain.model.SyncStatus
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.StateFlow
import kotlin.time.Duration
import kotlin.time.Duration.Companion.seconds

/**
 * Repository interface for managing devices, device types, and battery events.
 *
 * This is the primary data access abstraction in the domain layer. Implementations
 * coordinate between local storage and remote synchronization.
 *
 * ## Architecture
 * - Local storage is the source of truth for read operations
 * - Write operations update local storage and trigger async remote sync
 * - [syncStatus] exposes the current synchronization state
 *
 * ## Flow Semantics
 * All getter methods return [Flow]s that emit the current value immediately
 * and continue emitting updates as data changes. Collect these flows to
 * observe real-time data updates.
 */
interface DeviceRepository {
    /**
     * Current synchronization status with remote storage.
     * Observe this to show sync indicators or error messages in the UI.
     */
    val syncStatus: StateFlow<SyncStatus>

    /**
     * Resets sync status to [SyncStatus.Idle].
     * Call this to dismiss [SyncStatus.Success] or [SyncStatus.Failed] states
     * after the user has acknowledged them.
     */
    fun dismissSyncStatus()

    /**
     * Triggers an immediate remote resync (e.g. for pull-to-refresh), independent of the timing
     * of the background sync loop.
     *
     * @param timeout how long to wait for a remote update before giving up; the background sync
     *   loop keeps retrying regardless, so this only bounds how long *this* call waits. Defaults
     *   to a short timeout tuned for an interactive spinner (pull-to-refresh); callers with a
     *   naturally longer wait already in progress and a higher chance of hitting a cold backend
     *   (e.g. immediately after sign-in) may pass a longer one.
     * @return the typed terminal [SyncOutcome] of this attempt, so callers can distinguish
     *   success from auth-required from failure instead of inferring from side effects. The same
     *   terminal state is also published on [syncStatus].
     */
    suspend fun resync(timeout: Duration = DEFAULT_RESYNC_TIMEOUT): SyncOutcome

    /**
     * Deletes all locally cached devices, device types, and events for whichever environment is
     * currently selected. Does not touch the remote backend. Intended for sign-out: local storage
     * is isolated per data mode (see [com.chriscartland.batterybutler.domain.model.DataMode]),
     * so this only clears the currently-active environment's cache.
     */
    suspend fun clearAllLocalData()

    // region Device Operations

    /** Returns a flow of all devices, sorted by name. */
    fun getAllDevices(): Flow<List<Device>>

    /** Returns a flow of the device with the given [id], or null if not found. */
    fun getDeviceById(id: String): Flow<Device?>

    /** Adds a new device and triggers remote sync. */
    suspend fun addDevice(device: Device): Result<Unit, DataError>

    /** Updates an existing device and triggers remote sync. */
    suspend fun updateDevice(device: Device): Result<Unit, DataError>

    /** Deletes the device with the given [id] and triggers remote sync. */
    suspend fun deleteDevice(id: String): Result<Unit, DataError>

    // endregion

    // region DeviceType Operations

    /** Returns a flow of all device types, sorted by name. */
    fun getAllDeviceTypes(): Flow<List<DeviceType>>

    /** Returns a flow of the device type with the given [id], or null if not found. */
    fun getDeviceTypeById(id: String): Flow<DeviceType?>

    /** Adds a new device type and triggers remote sync. */
    suspend fun addDeviceType(type: DeviceType): Result<Unit, DataError>

    /** Updates an existing device type and triggers remote sync. */
    suspend fun updateDeviceType(type: DeviceType): Result<Unit, DataError>

    /** Deletes the device type with the given [id] and triggers remote sync. */
    suspend fun deleteDeviceType(id: String): Result<Unit, DataError>

    // endregion

    // region BatteryEvent Operations

    /** Returns a flow of all battery events for the device with [deviceId], sorted by date descending. */
    fun getEventsForDevice(deviceId: String): Flow<List<BatteryEvent>>

    /** Returns a flow of all battery events across all devices, sorted by date descending. */
    fun getAllEvents(): Flow<List<BatteryEvent>>

    /** Returns a flow of the battery event with the given [id], or null if not found. */
    fun getEventById(id: String): Flow<BatteryEvent?>

    /** Adds a new battery event and triggers remote sync. */
    suspend fun addEvent(event: BatteryEvent): Result<Unit, DataError>

    /** Updates an existing battery event and triggers remote sync. */
    suspend fun updateEvent(event: BatteryEvent): Result<Unit, DataError>

    /** Deletes the battery event with the given [id] and triggers remote sync. */
    suspend fun deleteEvent(id: String): Result<Unit, DataError>

    // endregion
}

/** Default [DeviceRepository.resync] timeout, tuned for an interactive pull-to-refresh spinner. */
val DEFAULT_RESYNC_TIMEOUT = 15.seconds
