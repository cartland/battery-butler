package com.chriscartland.batterybutler.networking

import com.chriscartland.batterybutler.domain.model.BatteryEvent
import com.chriscartland.batterybutler.domain.model.Device
import com.chriscartland.batterybutler.domain.model.DeviceType
import com.chriscartland.batterybutler.domain.repository.RemoteUpdate
import com.chriscartland.batterybutler.proto.ProtoBatteryEvent
import com.chriscartland.batterybutler.proto.ProtoDevice
import com.chriscartland.batterybutler.proto.ProtoDeviceType
import com.chriscartland.batterybutler.proto.SyncUpdate

internal object SyncMapper {
    fun toDomain(proto: SyncUpdate): RemoteUpdate =
        RemoteUpdate(
            isFullSnapshot = proto.is_full_snapshot,
            deviceTypes = proto.device_types.map { it.toDomain() },
            devices = proto.devices.map { it.toDomain() },
            events = proto.events.map { it.toDomain() },
        )

    fun toProto(domain: RemoteUpdate): SyncUpdate =
        SyncUpdate(
            is_full_snapshot = domain.isFullSnapshot,
            device_types = domain.deviceTypes.map { it.toProto() },
            devices = domain.devices.map { it.toProto() },
            events = domain.events.map { it.toProto() },
        )

    private fun ProtoDeviceType.toDomain(): DeviceType =
        DeviceType(
            id = id,
            name = name,
        )

    private fun DeviceType.toProto(): ProtoDeviceType =
        ProtoDeviceType(
            id = id,
            name = name,
        )

    private fun ProtoDevice.toDomain(): Device =
        Device(
            id = id,
            name = name,
            typeId = type_id,
            location = location,
            batteryLastReplaced = kotlinx.datetime.Instant.fromEpochMilliseconds(0),
            lastUpdated = kotlinx.datetime.Instant.fromEpochMilliseconds(0),
        )

    private fun Device.toProto(): ProtoDevice =
        ProtoDevice(
            id = id,
            name = name,
            type_id = typeId,
            location = location ?: "",
        )

    private fun ProtoBatteryEvent.toDomain(): BatteryEvent =
        BatteryEvent(
            id = id,
            deviceId = device_id,
            date = kotlinx.datetime.Instant.fromEpochMilliseconds(date_timestamp_ms),
            notes = notes,
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
