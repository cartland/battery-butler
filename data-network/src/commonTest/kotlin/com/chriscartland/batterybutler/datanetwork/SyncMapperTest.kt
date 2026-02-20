package com.chriscartland.batterybutler.datanetwork

import com.chriscartland.batterybutler.datanetwork.grpc.SyncMapper
import com.chriscartland.batterybutler.domain.model.BatteryEvent
import com.chriscartland.batterybutler.domain.model.Device
import com.chriscartland.batterybutler.domain.model.DeviceType
import com.chriscartland.batterybutler.domain.repository.RemoteUpdate
import com.chriscartland.batterybutler.proto.ProtoBatteryEvent
import com.chriscartland.batterybutler.proto.ProtoDevice
import com.chriscartland.batterybutler.proto.ProtoDeviceType
import com.chriscartland.batterybutler.proto.SyncUpdate
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertNull
import kotlin.time.Instant

class SyncMapperTest {
    @Test
    fun `toDomain maps Proto to Domain correctly`() {
        val proto = SyncUpdate(
            is_full_snapshot = true,
            device_types = listOf(
                ProtoDeviceType(
                    id = "type1",
                    name = "Type 1",
                ),
            ),
            devices = listOf(
                ProtoDevice(
                    id = "dev1",
                    name = "Device 1",
                    type_id = "type1",
                    location = "Loc 1",
                ),
            ),
            events = listOf(
                ProtoBatteryEvent(
                    id = "ev1",
                    device_id = "dev1",
                    date_timestamp_ms = 1704067200000, // 2024-01-01 UTC
                    created_timestamp_ms = 1704067200000,
                    notes = "Note 1",
                ),
            ),
        )

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

        assertEquals(false, proto.is_full_snapshot)
        assertEquals(1, proto.device_types.size)
        assertEquals("Type 1", proto.device_types[0].name)
        assertEquals(1, proto.devices.size)
        assertEquals("Loc 1", proto.devices[0].location)
        assertEquals(1, proto.events.size)
        assertEquals(1704067200000, proto.events[0].created_timestamp_ms)
        // Date check: 2024-01-01 at start of day in UTC is 1704067200000
        assertEquals(1704067200000, proto.events[0].date_timestamp_ms)
    }

    // --- Edge case: empty string → null ---

    @Test
    fun `toDomain maps empty location to null`() {
        val proto = ProtoDevice(
            id = "dev1",
            name = "Device 1",
            type_id = "type1",
            location = "",
        )
        val domain = SyncMapper.toDomain(
            SyncUpdate(devices = listOf(proto)),
        )
        assertNull(domain.devices[0].location)
    }

    @Test
    fun `toDomain maps empty defaultIcon to null`() {
        val proto = ProtoDeviceType(
            id = "type1",
            name = "Type 1",
            default_icon = "",
        )
        val domain = SyncMapper.toDomain(
            SyncUpdate(device_types = listOf(proto)),
        )
        assertNull(domain.deviceTypes[0].defaultIcon)
    }

    @Test
    fun `toDomain maps empty notes to null`() {
        val proto = ProtoBatteryEvent(
            id = "ev1",
            device_id = "dev1",
            date_timestamp_ms = 1704067200000,
            created_timestamp_ms = 1704067200000,
            notes = "",
        )
        val domain = SyncMapper.toDomain(
            SyncUpdate(events = listOf(proto)),
        )
        assertNull(domain.events[0].notes)
    }

    @Test
    fun `toDomain maps empty imagePath to null`() {
        val proto = ProtoDevice(
            id = "dev1",
            name = "Device 1",
            type_id = "type1",
            image_path = "",
        )
        val domain = SyncMapper.toDomain(
            SyncUpdate(devices = listOf(proto)),
        )
        assertNull(domain.devices[0].imagePath)
    }

    // --- Edge case: zero timestamps → epoch ---

    @Test
    fun `toDomain maps zero batteryLastReplaced to epoch`() {
        val proto = ProtoDevice(
            id = "dev1",
            name = "Device 1",
            type_id = "type1",
            battery_last_replaced_timestamp_ms = 0,
        )
        val domain = SyncMapper.toDomain(
            SyncUpdate(devices = listOf(proto)),
        )
        assertEquals(Instant.fromEpochMilliseconds(0), domain.devices[0].batteryLastReplaced)
    }

    @Test
    fun `toDomain maps zero lastUpdated to epoch`() {
        val proto = ProtoDevice(
            id = "dev1",
            name = "Device 1",
            type_id = "type1",
            last_updated_timestamp_ms = 0,
        )
        val domain = SyncMapper.toDomain(
            SyncUpdate(devices = listOf(proto)),
        )
        assertEquals(Instant.fromEpochMilliseconds(0), domain.devices[0].lastUpdated)
    }

    // --- Edge case: zero/empty defaults ---

    @Test
    fun `toDomain maps zero batteryQuantity to default 1`() {
        val proto = ProtoDeviceType(
            id = "type1",
            name = "Type 1",
            battery_quantity = 0,
        )
        val domain = SyncMapper.toDomain(
            SyncUpdate(device_types = listOf(proto)),
        )
        assertEquals(1, domain.deviceTypes[0].batteryQuantity)
    }

    @Test
    fun `toDomain maps empty batteryType to default AA`() {
        val proto = ProtoDeviceType(
            id = "type1",
            name = "Type 1",
            battery_type = "",
        )
        val domain = SyncMapper.toDomain(
            SyncUpdate(device_types = listOf(proto)),
        )
        assertEquals("AA", domain.deviceTypes[0].batteryType)
    }

    // --- Round-trip tests ---

    @Test
    fun `round-trip DeviceType preserves data`() {
        val original = DeviceType(
            id = "type1",
            name = "Smoke Alarm",
            defaultIcon = "detector_smoke",
            batteryType = "9V",
            batteryQuantity = 2,
        )
        val update = RemoteUpdate(
            isFullSnapshot = false,
            deviceTypes = listOf(original),
            devices = emptyList(),
            events = emptyList(),
        )

        val roundTripped = SyncMapper.toDomain(SyncMapper.toProto(update))

        assertEquals(original, roundTripped.deviceTypes[0])
    }

    @Test
    fun `round-trip Device preserves data`() {
        val original = Device(
            id = "dev1",
            name = "Kitchen Alarm",
            typeId = "type1",
            location = "Kitchen",
            batteryLastReplaced = Instant.fromEpochMilliseconds(1704067200000),
            lastUpdated = Instant.fromEpochMilliseconds(1704153600000),
            imagePath = "/images/alarm.jpg",
        )
        val update = RemoteUpdate(
            isFullSnapshot = false,
            deviceTypes = emptyList(),
            devices = listOf(original),
            events = emptyList(),
        )

        val roundTripped = SyncMapper.toDomain(SyncMapper.toProto(update))

        assertEquals(original, roundTripped.devices[0])
    }

    @Test
    fun `round-trip BatteryEvent preserves data`() {
        val original = BatteryEvent(
            id = "ev1",
            deviceId = "dev1",
            date = Instant.fromEpochMilliseconds(1704067200000),
            notes = "Replaced with Duracell",
        )
        val update = RemoteUpdate(
            isFullSnapshot = false,
            deviceTypes = emptyList(),
            devices = emptyList(),
            events = listOf(original),
        )

        val roundTripped = SyncMapper.toDomain(SyncMapper.toProto(update))

        assertEquals(original, roundTripped.events[0])
    }

    // --- Deleted IDs ---

    @Test
    fun `toDomain maps deleted IDs correctly`() {
        val proto = SyncUpdate(
            deleted_device_type_ids = listOf("dt1", "dt2"),
            deleted_device_ids = listOf("d1"),
            deleted_event_ids = listOf("e1", "e2", "e3"),
        )
        val domain = SyncMapper.toDomain(proto)
        assertEquals(listOf("dt1", "dt2"), domain.deletedDeviceTypeIds)
        assertEquals(listOf("d1"), domain.deletedDeviceIds)
        assertEquals(listOf("e1", "e2", "e3"), domain.deletedEventIds)
    }

    @Test
    fun `toProto maps deleted IDs correctly`() {
        val domain = RemoteUpdate(
            isFullSnapshot = false,
            deviceTypes = emptyList(),
            devices = emptyList(),
            events = emptyList(),
            deletedDeviceTypeIds = listOf("dt1"),
            deletedDeviceIds = listOf("d1", "d2"),
            deletedEventIds = listOf("e1"),
        )
        val proto = SyncMapper.toProto(domain)
        assertEquals(listOf("dt1"), proto.deleted_device_type_ids)
        assertEquals(listOf("d1", "d2"), proto.deleted_device_ids)
        assertEquals(listOf("e1"), proto.deleted_event_ids)
    }

    // --- Null domain → empty proto ---

    @Test
    fun `toProto maps null location to empty string`() {
        val domain = RemoteUpdate(
            isFullSnapshot = false,
            deviceTypes = emptyList(),
            devices = listOf(
                Device(
                    id = "dev1",
                    name = "Device 1",
                    typeId = "type1",
                    batteryLastReplaced = Instant.fromEpochMilliseconds(0),
                    lastUpdated = Instant.fromEpochMilliseconds(0),
                    location = null,
                ),
            ),
            events = emptyList(),
        )
        val proto = SyncMapper.toProto(domain)
        assertEquals("", proto.devices[0].location)
    }

    @Test
    fun `toProto maps null defaultIcon to empty string`() {
        val domain = RemoteUpdate(
            isFullSnapshot = false,
            deviceTypes = listOf(
                DeviceType(
                    id = "type1",
                    name = "Type 1",
                    defaultIcon = null,
                ),
            ),
            devices = emptyList(),
            events = emptyList(),
        )
        val proto = SyncMapper.toProto(domain)
        assertEquals("", proto.device_types[0].default_icon)
    }
}
