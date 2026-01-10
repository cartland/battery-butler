package com.chriscartland.batterybutler.networking

import com.chriscartland.batterybutler.domain.model.BatteryEvent
import com.chriscartland.batterybutler.domain.model.Device
import com.chriscartland.batterybutler.domain.model.DeviceType
import com.chriscartland.batterybutler.domain.repository.RemoteUpdate
import com.chriscartland.batterybutler.proto.ProtoBatteryEvent
import com.chriscartland.batterybutler.proto.ProtoDevice
import com.chriscartland.batterybutler.proto.ProtoDeviceType
import com.chriscartland.batterybutler.proto.SyncUpdate
import kotlinx.datetime.Instant
import org.junit.Test
import kotlin.test.assertEquals

class SyncMapperTest {
    @Test
    fun `toDomain maps Proto to Domain correctly`() {
        val proto = SyncUpdate
            .newBuilder()
            .setIsFullSnapshot(true)
            .addDeviceTypes(
                ProtoDeviceType
                    .newBuilder()
                    .setId("type1")
                    .setName("Type 1")
                    .build(),
            ).addDevices(
                ProtoDevice
                    .newBuilder()
                    .setId("dev1")
                    .setName("Device 1")
                    .setTypeId("type1")
                    .setLocation("Loc 1")
                    .build(),
            ).addEvents(
                ProtoBatteryEvent
                    .newBuilder()
                    .setId("ev1")
                    .setDeviceId("dev1")
                    .setDateTimestampMs(1704067200000) // 2024-01-01 UTC
                    .setCreatedTimestampMs(1704067200000)
                    .setNotes("Note 1")
                    .build(),
            ).build()

        val domain = SyncMapper.toDomain(proto)

        assertEquals(true, domain.isFullSnapshot)
        assertEquals(1, domain.deviceTypes.size)
        assertEquals("type1", domain.deviceTypes[0].id)
        assertEquals(1, domain.devices.size)
        assertEquals("Device 1", domain.devices[0].name)
        assertEquals(1, domain.events.size)
        assertEquals("Note 1", domain.events[0].notes)
    }

    @Test
    fun `toProto maps Domain to Proto correctly`() {
        val domain = RemoteUpdate(
            isFullSnapshot = false,
            deviceTypes = listOf(
                DeviceType(
                    id = "type1",
                    name = "Type 1",
                    defaultIcon = "icon",
                    batteryType = "AA",
                    batteryQuantity = 2,
                ),
            ),
            devices = listOf(
                Device(
                    id = "dev1",
                    name = "Device 1",
                    typeId = "type1",
                    batteryLastReplaced = Instant.fromEpochMilliseconds(0),
                    lastUpdated = Instant.fromEpochMilliseconds(0),
                    location = "Loc 1",
                ),
            ),
            events = listOf(
                BatteryEvent(
                    id = "ev1",
                    deviceId = "dev1",
                    date = Instant.fromEpochMilliseconds(1704067200000), // 2024-01-01 UTC
                    notes = "Note 1",
                ),
            ),
        )

        val proto = SyncMapper.toProto(domain)

        assertEquals(false, proto.isFullSnapshot)
        assertEquals(1, proto.deviceTypesCount)
        assertEquals("Type 1", proto.deviceTypesList[0].name)
        assertEquals(1, proto.devicesCount)
        assertEquals("Loc 1", proto.devicesList[0].location)
        assertEquals(1, proto.eventsCount)
        assertEquals(1704067200000, proto.eventsList[0].createdTimestampMs)
        // Date check: 2024-01-01 at start of day in UTC is 1704067200000
        assertEquals(1704067200000, proto.eventsList[0].dateTimestampMs)
    }
}
