package com.chriscartland.batterybutler.server.app

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

// Duplicated from Networking module for now, as Server doesn't depend on Networking
@OptIn(ExperimentalTime::class)
internal object ServerSyncMapper {
    fun toDomain(proto: SyncUpdate): RemoteUpdate =
        RemoteUpdate(
            isFullSnapshot = proto.isFullSnapshot,
            deviceTypes = proto.deviceTypesList.map { it.toDomain() },
            devices = proto.devicesList.map { it.toDomain() },
            events = proto.eventsList.map { it.toDomain() },
        )

    fun toProto(domain: RemoteUpdate): SyncUpdate =
        SyncUpdate
            .newBuilder()
            .setIsFullSnapshot(domain.isFullSnapshot)
            .addAllDeviceTypes(domain.deviceTypes.map { it.toProto() })
            .addAllDevices(domain.devices.map { it.toProto() })
            .addAllEvents(domain.events.map { it.toProto() })
            .build()

    private fun ProtoDeviceType.toDomain(): DeviceType =
        DeviceType(
            id = id,
            name = name,
            defaultIcon = defaultIcon.takeIf { it.isNotEmpty() },
            batteryType = batteryType.takeIf { it.isNotEmpty() } ?: "AA",
            batteryQuantity = batteryQuantity.takeIf { it > 0 } ?: 1,
        )

    private fun DeviceType.toProto(): ProtoDeviceType =
        ProtoDeviceType
            .newBuilder()
            .setId(id)
            .setName(name)
            .setDefaultIcon(defaultIcon ?: "")
            .setBatteryType(batteryType)
            .setBatteryQuantity(batteryQuantity)
            .build()

    private fun ProtoDevice.toDomain(): Device =
        Device(
            id = id,
            name = name,
            typeId = typeId,
            location = location.takeIf { it.isNotEmpty() },
            batteryLastReplaced = batteryLastReplacedTimestampMs
                .takeIf { it > 0 }
                ?.let { Instant.fromEpochMilliseconds(it) }
                ?: Instant.fromEpochMilliseconds(0),
            lastUpdated = lastUpdatedTimestampMs
                .takeIf { it > 0 }
                ?.let { Instant.fromEpochMilliseconds(it) }
                ?: Instant.fromEpochMilliseconds(0),
        )

    private fun Device.toProto(): ProtoDevice =
        ProtoDevice
            .newBuilder()
            .setId(id)
            .setName(name)
            .setTypeId(typeId)
            .setLocation(location ?: "")
            .setBatteryLastReplacedTimestampMs(batteryLastReplaced.toEpochMilliseconds())
            .setLastUpdatedTimestampMs(lastUpdated.toEpochMilliseconds())
            .build()

    private fun ProtoBatteryEvent.toDomain(): BatteryEvent =
        BatteryEvent(
            id = id,
            deviceId = deviceId,
            date = Instant.fromEpochMilliseconds(dateTimestampMs),
            notes = notes,
        )

    private fun BatteryEvent.toProto(): ProtoBatteryEvent =
        ProtoBatteryEvent
            .newBuilder()
            .setId(id)
            .setDeviceId(deviceId)
            .setDateTimestampMs(date.toEpochMilliseconds())
            .setCreatedTimestampMs(date.toEpochMilliseconds())
            .setNotes(notes ?: "")
            .build()
}
