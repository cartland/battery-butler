package com.chriscartland.batterybutler.domain.repository

import com.chriscartland.batterybutler.domain.model.BatteryEvent
import com.chriscartland.batterybutler.domain.model.Device
import com.chriscartland.batterybutler.domain.model.DeviceType
import com.chriscartland.batterybutler.domain.model.SyncStatus
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.StateFlow

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

    // region Device Operations

    /** Returns a flow of all devices, sorted by name. */
    fun getAllDevices(): Flow<List<Device>>

    /** Returns a flow of the device with the given [id], or null if not found. */
    fun getDeviceById(id: String): Flow<Device?>

    /** Adds a new device and triggers remote sync. */
    suspend fun addDevice(device: Device)

    /** Updates an existing device and triggers remote sync. */
    suspend fun updateDevice(device: Device)

    /** Deletes the device with the given [id] and triggers remote sync. */
    suspend fun deleteDevice(id: String)

    // endregion

    // region DeviceType Operations

    /** Returns a flow of all device types, sorted by name. */
    fun getAllDeviceTypes(): Flow<List<DeviceType>>

    /** Returns a flow of the device type with the given [id], or null if not found. */
    fun getDeviceTypeById(id: String): Flow<DeviceType?>

    /** Adds a new device type and triggers remote sync. */
    suspend fun addDeviceType(type: DeviceType)

    /** Updates an existing device type and triggers remote sync. */
    suspend fun updateDeviceType(type: DeviceType)

    /** Deletes the device type with the given [id] and triggers remote sync. */
    suspend fun deleteDeviceType(id: String)

    // endregion

    // region BatteryEvent Operations

    /** Returns a flow of all battery events for the device with [deviceId], sorted by date descending. */
    fun getEventsForDevice(deviceId: String): Flow<List<BatteryEvent>>

    /** Returns a flow of all battery events across all devices, sorted by date descending. */
    fun getAllEvents(): Flow<List<BatteryEvent>>

    /** Returns a flow of the battery event with the given [id], or null if not found. */
    fun getEventById(id: String): Flow<BatteryEvent?>

    /** Adds a new battery event and triggers remote sync. */
    suspend fun addEvent(event: BatteryEvent)

    /** Updates an existing battery event and triggers remote sync. */
    suspend fun updateEvent(event: BatteryEvent)

    /** Deletes the battery event with the given [id] and triggers remote sync. */
    suspend fun deleteEvent(id: String)

    // endregion
}
