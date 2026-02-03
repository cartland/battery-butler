package com.chriscartland.batterybutler.datanetwork.grpc

import com.chriscartland.batterybutler.domain.model.BatteryEvent
import com.chriscartland.batterybutler.domain.model.Device
import com.chriscartland.batterybutler.domain.model.DeviceType
import com.chriscartland.batterybutler.domain.repository.RemoteUpdate
import com.chriscartland.batterybutler.proto.ProtoBatteryEvent
import com.chriscartland.batterybutler.proto.ProtoDevice
import com.chriscartland.batterybutler.proto.ProtoDeviceType
import com.chriscartland.batterybutler.proto.SyncUpdate
import kotlin.time.ExperimentalTime
import kotlin.time.Instant

/**
 * Maps between Protocol Buffer types and domain models for sync operations.
 *
 * This mapper handles the conversion between the wire format (protobuf) and
 * the application's domain models. It applies sensible defaults when proto
 * fields are missing or have zero values.
 */
@OptIn(ExperimentalTime::class)
internal object SyncMapper {
    /**
     * Converts a [SyncUpdate] proto message to a [RemoteUpdate] domain object.
     */
    fun toDomain(proto: SyncUpdate): RemoteUpdate =
        RemoteUpdate(
            isFullSnapshot = proto.is_full_snapshot,
            deviceTypes = proto.device_types.map { it.toDomain() },
            devices = proto.devices.map { it.toDomain() },
            events = proto.events.map { it.toDomain() },
            deletedDeviceTypeIds = proto.deleted_device_type_ids,
            deletedDeviceIds = proto.deleted_device_ids,
            deletedEventIds = proto.deleted_event_ids,
        )

    /**
     * Converts a [RemoteUpdate] domain object to a [SyncUpdate] proto message.
     */
    fun toProto(domain: RemoteUpdate): SyncUpdate =
        SyncUpdate(
            is_full_snapshot = domain.isFullSnapshot,
            device_types = domain.deviceTypes.map { it.toProto() },
            devices = domain.devices.map { it.toProto() },
            events = domain.events.map { it.toProto() },
            deleted_device_type_ids = domain.deletedDeviceTypeIds,
            deleted_device_ids = domain.deletedDeviceIds,
            deleted_event_ids = domain.deletedEventIds,
        )

    private fun ProtoDeviceType.toDomain(): DeviceType =
        DeviceType(
            id = id,
            name = name,
            defaultIcon = default_icon.takeIf { it.isNotEmpty() },
            batteryType = battery_type.takeIf { it.isNotEmpty() } ?: "AA",
            batteryQuantity = battery_quantity.takeIf { it > 0 } ?: 1,
        )

    private fun DeviceType.toProto(): ProtoDeviceType =
        ProtoDeviceType(
            id = id,
            name = name,
            default_icon = defaultIcon ?: "",
            battery_type = batteryType,
            battery_quantity = batteryQuantity,
        )

    private fun ProtoDevice.toDomain(): Device =
        Device(
            id = id,
            name = name,
            typeId = type_id,
            location = location.takeIf { it.isNotEmpty() },
            batteryLastReplaced = battery_last_replaced_timestamp_ms
                .takeIf { it > 0 }
                ?.let { Instant.fromEpochMilliseconds(it) }
                ?: Instant.fromEpochMilliseconds(0),
            lastUpdated = last_updated_timestamp_ms
                .takeIf { it > 0 }
                ?.let { Instant.fromEpochMilliseconds(it) }
                ?: Instant.fromEpochMilliseconds(0),
            imagePath = image_path.takeIf { it.isNotEmpty() },
        )

    private fun Device.toProto(): ProtoDevice =
        ProtoDevice(
            id = id,
            name = name,
            type_id = typeId,
            location = location ?: "",
            battery_last_replaced_timestamp_ms = batteryLastReplaced.toEpochMilliseconds(),
            last_updated_timestamp_ms = lastUpdated.toEpochMilliseconds(),
            image_path = imagePath ?: "",
        )

    private fun ProtoBatteryEvent.toDomain(): BatteryEvent =
        BatteryEvent(
            id = id,
            deviceId = device_id,
            date = Instant.fromEpochMilliseconds(date_timestamp_ms),
            notes = notes.takeIf { it.isNotEmpty() },
        )

    private fun BatteryEvent.toProto(): ProtoBatteryEvent =
        ProtoBatteryEvent(
            id = id,
            device_id = deviceId,
            date_timestamp_ms = date.toEpochMilliseconds(),
            created_timestamp_ms = date.toEpochMilliseconds(),
            notes = notes ?: "",
        )
}
