package com.chriscartland.batterybutler.datanetwork.rest

import com.chriscartland.batterybutler.domain.model.BatteryEvent
import com.chriscartland.batterybutler.domain.model.Device
import com.chriscartland.batterybutler.domain.model.DeviceType
import com.chriscartland.batterybutler.domain.repository.RemoteUpdate
import kotlin.time.ExperimentalTime
import kotlin.time.Instant

/*
 * Maps between the REST sync wire DTOs ([SyncDto]) and the domain models.
 *
 * Mirrors the gRPC-side `grpc/SyncMapper` conventions EXACTLY so both transports yield
 * identical domain objects:
 *   - empty string  -> null              (defaultIcon, location, imagePath, notes)
 *   - zero timestamp -> Instant epoch(0) (batteryLastReplaced, lastUpdated)
 *   - batteryQuantity <= 0 -> 1
 *   - empty batteryType    -> "AA"
 *
 * Field asymmetries (same as SyncMapper): the wire's `createdTimestampMs` has no domain field
 * (dropped on read; set to the event date on write); the domain's `batteryType` on an event has
 * no wire field (dropped on write).
 */
@OptIn(ExperimentalTime::class)
internal object RestSyncMapper {
    /** `GET /sync` snapshot -> a full-snapshot [RemoteUpdate]. */
    fun toRemoteUpdate(snapshot: SyncSnapshotWire): RemoteUpdate =
        RemoteUpdate(
            isFullSnapshot = true,
            deviceTypes = snapshot.deviceTypes.map { it.toDomain() },
            devices = snapshot.devices.map { it.toDomain() },
            events = snapshot.events.map { it.toDomain() },
        )

    /** A [RemoteUpdate] -> the `POST /sync` patch. */
    fun toPushRequest(update: RemoteUpdate): SyncPushRequestWire =
        SyncPushRequestWire(
            deviceTypes = update.deviceTypes.map { it.toWire() },
            devices = update.devices.map { it.toWire() },
            events = update.events.map { it.toWire() },
            deletedDeviceTypeIds = update.deletedDeviceTypeIds,
            deletedDeviceIds = update.deletedDeviceIds,
            deletedEventIds = update.deletedEventIds,
        )

    private fun DeviceTypeWire.toDomain(): DeviceType =
        DeviceType(
            id = id,
            name = name,
            defaultIcon = defaultIcon.takeIf { it.isNotEmpty() },
            batteryType = batteryType.takeIf { it.isNotEmpty() } ?: "AA",
            batteryQuantity = batteryQuantity.takeIf { it > 0 } ?: 1,
        )

    private fun DeviceType.toWire(): DeviceTypeWire =
        DeviceTypeWire(
            id = id,
            name = name,
            defaultIcon = defaultIcon ?: "",
            batteryType = batteryType,
            batteryQuantity = batteryQuantity,
        )

    private fun DeviceWire.toDomain(): Device =
        Device(
            id = id,
            name = name,
            typeId = typeId,
            location = location.takeIf { it.isNotEmpty() },
            batteryLastReplaced = batteryLastReplacedTimestampMs.toInstantOrEpoch(),
            lastUpdated = lastUpdatedTimestampMs.toInstantOrEpoch(),
            imagePath = imagePath.takeIf { it.isNotEmpty() },
        )

    private fun Device.toWire(): DeviceWire =
        DeviceWire(
            id = id,
            name = name,
            typeId = typeId,
            location = location ?: "",
            batteryLastReplacedTimestampMs = batteryLastReplaced.toEpochMilliseconds(),
            lastUpdatedTimestampMs = lastUpdated.toEpochMilliseconds(),
            imagePath = imagePath ?: "",
        )

    private fun BatteryEventWire.toDomain(): BatteryEvent =
        BatteryEvent(
            id = id,
            deviceId = deviceId,
            date = Instant.fromEpochMilliseconds(dateTimestampMs),
            notes = notes.takeIf { it.isNotEmpty() },
        )

    private fun BatteryEvent.toWire(): BatteryEventWire =
        BatteryEventWire(
            id = id,
            deviceId = deviceId,
            dateTimestampMs = date.toEpochMilliseconds(),
            createdTimestampMs = date.toEpochMilliseconds(),
            notes = notes ?: "",
        )

    private fun Long.toInstantOrEpoch(): Instant = takeIf { it > 0 }?.let { Instant.fromEpochMilliseconds(it) } ?: Instant.fromEpochMilliseconds(0)
}
