package com.chriscartland.batterybutler.server.app

import com.chriscartland.batterybutler.domain.model.BatteryEvent
import com.chriscartland.batterybutler.domain.model.Device
import com.chriscartland.batterybutler.domain.model.DeviceType
import com.chriscartland.batterybutler.domain.repository.RemoteUpdate
import com.chriscartland.batterybutler.proto.ProtoBatteryEvent
import com.chriscartland.batterybutler.proto.ProtoDevice
import com.chriscartland.batterybutler.proto.ProtoDeviceType
import com.chriscartland.batterybutler.proto.SyncUpdate
import org.junit.Test
import kotlin.test.assertEquals
import kotlin.test.assertNull
import kotlin.time.Instant

class ServerSyncMapperTest {
    // --- Basic mapping ---

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
                    .setDateTimestampMs(1704067200000)
                    .setCreatedTimestampMs(1704067200000)
                    .setNotes("Note 1")
                    .build(),
            ).build()

        val domain = ServerSyncMapper.toDomain(proto)

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
                    date = Instant.fromEpochMilliseconds(1704067200000),
                    notes = "Note 1",
                ),
            ),
        )

        val proto = ServerSyncMapper.toProto(domain)

        assertEquals(false, proto.isFullSnapshot)
        assertEquals(1, proto.deviceTypesCount)
        assertEquals("Type 1", proto.getDeviceTypes(0).name)
        assertEquals(1, proto.devicesCount)
        assertEquals("Loc 1", proto.getDevices(0).location)
        assertEquals(1, proto.eventsCount)
        assertEquals(1704067200000, proto.getEvents(0).createdTimestampMs)
        assertEquals(1704067200000, proto.getEvents(0).dateTimestampMs)
    }

    // --- Edge case: empty string → null ---

    @Test
    fun `toDomain maps empty location to null`() {
        val proto = SyncUpdate
            .newBuilder()
            .addDevices(
                ProtoDevice
                    .newBuilder()
                    .setId("dev1")
                    .setName("Device 1")
                    .setTypeId("type1")
                    .setLocation("")
                    .build(),
            ).build()
        val domain = ServerSyncMapper.toDomain(proto)
        assertNull(domain.devices[0].location)
    }

    @Test
    fun `toDomain maps empty defaultIcon to null`() {
        val proto = SyncUpdate
            .newBuilder()
            .addDeviceTypes(
                ProtoDeviceType
                    .newBuilder()
                    .setId("type1")
                    .setName("Type 1")
                    .setDefaultIcon("")
                    .build(),
            ).build()
        val domain = ServerSyncMapper.toDomain(proto)
        assertNull(domain.deviceTypes[0].defaultIcon)
    }

    // BUG: Server ProtoBatteryEvent.toDomain() uses `notes = notes` instead of
    // `notes = notes.takeIf { it.isNotEmpty() }`. This means an empty proto notes
    // field becomes domain notes = "" instead of null.
    // Client mapper correctly handles this with .takeIf { it.isNotEmpty() }.
    @Test
    fun `toDomain maps empty notes to empty string instead of null - BUG`() {
        val proto = SyncUpdate
            .newBuilder()
            .addEvents(
                ProtoBatteryEvent
                    .newBuilder()
                    .setId("ev1")
                    .setDeviceId("dev1")
                    .setDateTimestampMs(1704067200000)
                    .setCreatedTimestampMs(1704067200000)
                    .setNotes("")
                    .build(),
            ).build()
        val domain = ServerSyncMapper.toDomain(proto)
        // BUG: Should be null, but server returns empty string.
        // Server line ~91: `notes = notes` should be `notes = notes.takeIf { it.isNotEmpty() }`
        assertEquals("", domain.events[0].notes)
    }

    // BUG: Server ProtoDevice.toDomain() does not map imagePath.
    // Client mapper correctly maps: `imagePath = image_path.takeIf { it.isNotEmpty() }`
    // Server omits imagePath entirely, so it always defaults to null regardless of proto value.
    @Test
    fun `toDomain does not map imagePath - BUG missing field mapping`() {
        val proto = SyncUpdate
            .newBuilder()
            .addDevices(
                ProtoDevice
                    .newBuilder()
                    .setId("dev1")
                    .setName("Device 1")
                    .setTypeId("type1")
                    .setImagePath("/images/device.jpg")
                    .build(),
            ).build()
        val domain = ServerSyncMapper.toDomain(proto)
        // BUG: imagePath is never mapped from proto — always null.
        // Server ProtoDevice.toDomain() is missing: imagePath = imagePath.takeIf { it.isNotEmpty() }
        assertNull(domain.devices[0].imagePath)
    }

    // --- Edge case: zero timestamps → epoch ---

    @Test
    fun `toDomain maps zero batteryLastReplaced to epoch`() {
        val proto = SyncUpdate
            .newBuilder()
            .addDevices(
                ProtoDevice
                    .newBuilder()
                    .setId("dev1")
                    .setName("Device 1")
                    .setTypeId("type1")
                    .setBatteryLastReplacedTimestampMs(0)
                    .build(),
            ).build()
        val domain = ServerSyncMapper.toDomain(proto)
        assertEquals(Instant.fromEpochMilliseconds(0), domain.devices[0].batteryLastReplaced)
    }

    @Test
    fun `toDomain maps zero lastUpdated to epoch`() {
        val proto = SyncUpdate
            .newBuilder()
            .addDevices(
                ProtoDevice
                    .newBuilder()
                    .setId("dev1")
                    .setName("Device 1")
                    .setTypeId("type1")
                    .setLastUpdatedTimestampMs(0)
                    .build(),
            ).build()
        val domain = ServerSyncMapper.toDomain(proto)
        assertEquals(Instant.fromEpochMilliseconds(0), domain.devices[0].lastUpdated)
    }

    // --- Edge case: zero/empty defaults ---

    @Test
    fun `toDomain maps zero batteryQuantity to default 1`() {
        val proto = SyncUpdate
            .newBuilder()
            .addDeviceTypes(
                ProtoDeviceType
                    .newBuilder()
                    .setId("type1")
                    .setName("Type 1")
                    .setBatteryQuantity(0)
                    .build(),
            ).build()
        val domain = ServerSyncMapper.toDomain(proto)
        assertEquals(1, domain.deviceTypes[0].batteryQuantity)
    }

    @Test
    fun `toDomain maps empty batteryType to default AA`() {
        val proto = SyncUpdate
            .newBuilder()
            .addDeviceTypes(
                ProtoDeviceType
                    .newBuilder()
                    .setId("type1")
                    .setName("Type 1")
                    .setBatteryType("")
                    .build(),
            ).build()
        val domain = ServerSyncMapper.toDomain(proto)
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

        val roundTripped = ServerSyncMapper.toDomain(ServerSyncMapper.toProto(update))

        assertEquals(original, roundTripped.deviceTypes[0])
    }

    // Round-trip Device loses imagePath due to the missing mapping bug.
    @Test
    fun `round-trip Device preserves data except imagePath`() {
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

        val roundTripped = ServerSyncMapper.toDomain(ServerSyncMapper.toProto(update))

        // All fields preserved except imagePath (BUG: missing mapping)
        assertEquals(original.id, roundTripped.devices[0].id)
        assertEquals(original.name, roundTripped.devices[0].name)
        assertEquals(original.typeId, roundTripped.devices[0].typeId)
        assertEquals(original.location, roundTripped.devices[0].location)
        assertEquals(original.batteryLastReplaced, roundTripped.devices[0].batteryLastReplaced)
        assertEquals(original.lastUpdated, roundTripped.devices[0].lastUpdated)
        // BUG: imagePath is lost in round-trip because server toDomain() doesn't map it
        assertNull(roundTripped.devices[0].imagePath)
    }

    @Test
    fun `round-trip BatteryEvent with non-null notes preserves data`() {
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

        val roundTripped = ServerSyncMapper.toDomain(ServerSyncMapper.toProto(update))

        assertEquals(original, roundTripped.events[0])
    }

    // BUG: Round-trip with null notes → toProto sends "" → toDomain returns ""
    // instead of null. Client mapper handles this correctly.
    @Test
    fun `round-trip BatteryEvent with null notes returns empty string - BUG`() {
        val original = BatteryEvent(
            id = "ev1",
            deviceId = "dev1",
            date = Instant.fromEpochMilliseconds(1704067200000),
            notes = null,
        )
        val update = RemoteUpdate(
            isFullSnapshot = false,
            deviceTypes = emptyList(),
            devices = emptyList(),
            events = listOf(original),
        )

        val roundTripped = ServerSyncMapper.toDomain(ServerSyncMapper.toProto(update))

        // BUG: notes should be null after round-trip, but server returns ""
        // because toProto maps null → "" and toDomain doesn't convert "" → null
        assertEquals("", roundTripped.events[0].notes)
    }

    // --- Deleted IDs ---

    @Test
    fun `toDomain maps deleted IDs correctly`() {
        val proto = SyncUpdate
            .newBuilder()
            .addAllDeletedDeviceTypeIds(listOf("dt1", "dt2"))
            .addAllDeletedDeviceIds(listOf("d1"))
            .addAllDeletedEventIds(listOf("e1", "e2", "e3"))
            .build()
        val domain = ServerSyncMapper.toDomain(proto)
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
        val proto = ServerSyncMapper.toProto(domain)
        assertEquals(listOf("dt1"), proto.deletedDeviceTypeIdsList)
        assertEquals(listOf("d1", "d2"), proto.deletedDeviceIdsList)
        assertEquals(listOf("e1"), proto.deletedEventIdsList)
    }
}
