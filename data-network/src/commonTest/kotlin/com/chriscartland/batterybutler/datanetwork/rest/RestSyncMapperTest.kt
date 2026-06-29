package com.chriscartland.batterybutler.datanetwork.rest

import com.chriscartland.batterybutler.domain.model.BatteryEvent
import com.chriscartland.batterybutler.domain.model.Device
import com.chriscartland.batterybutler.domain.model.DeviceType
import com.chriscartland.batterybutler.domain.repository.RemoteUpdate
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertNull
import kotlin.test.assertTrue
import kotlin.time.Instant

/**
 * Mirrors `grpc/SyncMapperTest` for the REST wire DTOs: the two transports must produce
 * identical domain objects, so the same empty->null / zero->epoch / default conventions apply.
 */
class RestSyncMapperTest {
    @Test
    fun `toRemoteUpdate maps a snapshot to a full-snapshot RemoteUpdate`() {
        val snapshot = SyncSnapshotWire(
            deviceTypes = listOf(DeviceTypeWire(id = "type1", name = "Type 1")),
            devices = listOf(DeviceWire(id = "dev1", name = "Device 1", typeId = "type1", location = "Loc 1")),
            events = listOf(BatteryEventWire(id = "ev1", deviceId = "dev1", dateTimestampMs = 1_704_067_200_000, notes = "Note 1")),
        )

        val domain = RestSyncMapper.toRemoteUpdate(snapshot)

        assertTrue(domain.isFullSnapshot)
        assertEquals("type1", domain.deviceTypes.single().id)
        assertEquals("Device 1", domain.devices.single().name)
        assertEquals("Note 1", domain.events.single().notes)
    }

    @Test
    fun `toPushRequest maps a RemoteUpdate to a patch`() {
        val update = RemoteUpdate(
            isFullSnapshot = false,
            deviceTypes = listOf(DeviceType(id = "type1", name = "Type 1", defaultIcon = "icon", batteryType = "AA", batteryQuantity = 2)),
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
            events = listOf(BatteryEvent(id = "ev1", deviceId = "dev1", date = Instant.fromEpochMilliseconds(1_704_067_200_000), notes = "Note 1")),
        )

        val wire = RestSyncMapper.toPushRequest(update)

        assertEquals(2, wire.deviceTypes.single().batteryQuantity)
        assertEquals("Loc 1", wire.devices.single().location)
        // createdTimestampMs has no domain field; SyncMapper sets it to the event date.
        assertEquals(1_704_067_200_000, wire.events.single().dateTimestampMs)
        assertEquals(1_704_067_200_000, wire.events.single().createdTimestampMs)
    }

    // --- empty string -> null ---

    @Test
    fun `empty location, defaultIcon, notes, imagePath map to null`() {
        val snapshot = SyncSnapshotWire(
            deviceTypes = listOf(DeviceTypeWire(id = "t", name = "T", defaultIcon = "")),
            devices = listOf(DeviceWire(id = "d", name = "D", typeId = "t", location = "", imagePath = "")),
            events = listOf(BatteryEventWire(id = "e", deviceId = "d", dateTimestampMs = 1, notes = "")),
        )
        val domain = RestSyncMapper.toRemoteUpdate(snapshot)
        assertNull(domain.deviceTypes.single().defaultIcon)
        assertNull(domain.devices.single().location)
        assertNull(domain.devices.single().imagePath)
        assertNull(domain.events.single().notes)
    }

    // --- zero / empty defaults ---

    @Test
    fun `zero timestamps map to epoch`() {
        val device = DeviceWire(id = "d", name = "D", typeId = "t", batteryLastReplacedTimestampMs = 0, lastUpdatedTimestampMs = 0)
        val domain = RestSyncMapper.toRemoteUpdate(SyncSnapshotWire(devices = listOf(device)))
        assertEquals(Instant.fromEpochMilliseconds(0), domain.devices.single().batteryLastReplaced)
        assertEquals(Instant.fromEpochMilliseconds(0), domain.devices.single().lastUpdated)
    }

    @Test
    fun `zero batteryQuantity maps to 1 and empty batteryType maps to AA`() {
        val type = DeviceTypeWire(id = "t", name = "T", batteryType = "", batteryQuantity = 0)
        val domain = RestSyncMapper.toRemoteUpdate(SyncSnapshotWire(deviceTypes = listOf(type)))
        assertEquals(1, domain.deviceTypes.single().batteryQuantity)
        assertEquals("AA", domain.deviceTypes.single().batteryType)
    }

    // --- null domain -> empty wire ---

    @Test
    fun `null location and defaultIcon map to empty string on the wire`() {
        val update = RemoteUpdate(
            isFullSnapshot = false,
            deviceTypes = listOf(DeviceType(id = "t", name = "T", defaultIcon = null)),
            devices = listOf(
                Device(
                    id = "d",
                    name = "D",
                    typeId = "t",
                    batteryLastReplaced = Instant.fromEpochMilliseconds(0),
                    lastUpdated = Instant.fromEpochMilliseconds(0),
                    location = null,
                ),
            ),
            events = emptyList(),
        )
        val wire = RestSyncMapper.toPushRequest(update)
        assertEquals("", wire.deviceTypes.single().defaultIcon)
        assertEquals("", wire.devices.single().location)
    }

    // --- round trips (domain -> wire -> domain) ---

    @Test
    fun `round-trip preserves DeviceType, Device, and BatteryEvent`() {
        val update = RemoteUpdate(
            isFullSnapshot = true,
            deviceTypes = listOf(DeviceType(id = "t1", name = "Smoke Alarm", defaultIcon = "detector_smoke", batteryType = "9V", batteryQuantity = 2)),
            devices = listOf(
                Device(
                    id = "d1",
                    name = "Kitchen Alarm",
                    typeId = "t1",
                    location = "Kitchen",
                    batteryLastReplaced = Instant.fromEpochMilliseconds(1_704_067_200_000),
                    lastUpdated = Instant.fromEpochMilliseconds(1_704_153_600_000),
                    imagePath = "/img/a.jpg",
                ),
            ),
            events = listOf(BatteryEvent(id = "e1", deviceId = "d1", date = Instant.fromEpochMilliseconds(1_704_067_200_000), notes = "Duracell")),
        )

        // wire shape carries no isFullSnapshot; toRemoteUpdate always marks a snapshot full.
        val roundTripped = RestSyncMapper.toRemoteUpdate(
            SyncSnapshotWire(
                deviceTypes = RestSyncMapper.toPushRequest(update).deviceTypes,
                devices = RestSyncMapper.toPushRequest(update).devices,
                events = RestSyncMapper.toPushRequest(update).events,
            ),
        )

        assertEquals(update.deviceTypes.single(), roundTripped.deviceTypes.single())
        assertEquals(update.devices.single(), roundTripped.devices.single())
        assertEquals(update.events.single(), roundTripped.events.single())
    }

    // --- deleted ids ---

    @Test
    fun `toPushRequest carries deleted ids`() {
        val update = RemoteUpdate(
            isFullSnapshot = false,
            deviceTypes = emptyList(),
            devices = emptyList(),
            events = emptyList(),
            deletedDeviceTypeIds = listOf("dt1"),
            deletedDeviceIds = listOf("d1", "d2"),
            deletedEventIds = listOf("e1"),
        )
        val wire = RestSyncMapper.toPushRequest(update)
        assertEquals(listOf("dt1"), wire.deletedDeviceTypeIds)
        assertEquals(listOf("d1", "d2"), wire.deletedDeviceIds)
        assertEquals(listOf("e1"), wire.deletedEventIds)
    }
}
