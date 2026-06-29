package com.chriscartland.batterybutler.datanetwork.rest

import kotlinx.serialization.encodeToString
import kotlinx.serialization.json.Json
import kotlinx.serialization.json.jsonObject
import kotlin.test.Test
import kotlin.test.assertEquals

/**
 * Pins the REST sync wire contract on the client side. The mirror on the backend is
 * `services/labs-backend/test/batteryButlerWireContract.test.ts`. A change to a field name,
 * the field set, or a proto3 default must update BOTH — that is the point of pinning it on
 * each side. See [SyncDto] (the "defensive boundary" doc) for why.
 */
class SyncWireContractTest {
    // Producer config: emit the canonical shape (every field present) so a push is unambiguous.
    private val canonical = Json { encodeDefaults = true }

    // Consumer config: parse leniently (ignore unknown keys, apply defaults for missing ones).
    private val lenient = Json { ignoreUnknownKeys = true }

    private inline fun <reified T> keysOf(value: T): Set<String> = canonical.parseToJsonElement(canonical.encodeToString(value)).jsonObject.keys

    // region field sets — the exact JSON keys each shape serializes to

    @Test
    fun `DeviceTypeWire field set is pinned`() {
        assertEquals(
            setOf("id", "name", "defaultIcon", "batteryType", "batteryQuantity"),
            keysOf(DeviceTypeWire(id = "x")),
        )
    }

    @Test
    fun `DeviceWire field set is pinned`() {
        assertEquals(
            setOf(
                "id",
                "name",
                "typeId",
                "location",
                "batteryLastReplacedTimestampMs",
                "lastUpdatedTimestampMs",
                "imagePath",
            ),
            keysOf(DeviceWire(id = "x")),
        )
    }

    @Test
    fun `BatteryEventWire field set is pinned`() {
        assertEquals(
            setOf("id", "deviceId", "dateTimestampMs", "createdTimestampMs", "notes"),
            keysOf(BatteryEventWire(id = "x")),
        )
    }

    @Test
    fun `SyncSnapshotWire field set is pinned`() {
        assertEquals(
            setOf("deviceTypes", "devices", "events"),
            keysOf(SyncSnapshotWire()),
        )
    }

    @Test
    fun `SyncPushRequestWire field set is pinned`() {
        assertEquals(
            setOf(
                "deviceTypes",
                "devices",
                "events",
                "deletedDeviceTypeIds",
                "deletedDeviceIds",
                "deletedEventIds",
            ),
            keysOf(SyncPushRequestWire()),
        )
    }

    @Test
    fun `SyncPushResponseWire field set is pinned`() {
        assertEquals(
            setOf("success", "message"),
            keysOf(SyncPushResponseWire()),
        )
    }

    // endregion

    // region proto3 defaults — a peer that omits a field must parse to these values

    @Test
    fun `DeviceTypeWire applies proto3 defaults`() {
        val parsed = lenient.decodeFromString<DeviceTypeWire>("""{"id":"x"}""")
        assertEquals(DeviceTypeWire(id = "x", name = "", defaultIcon = "", batteryType = "AA", batteryQuantity = 1), parsed)
    }

    @Test
    fun `DeviceWire applies proto3 defaults`() {
        val parsed = lenient.decodeFromString<DeviceWire>("""{"id":"x"}""")
        assertEquals(
            DeviceWire(
                id = "x",
                name = "",
                typeId = "",
                location = "",
                batteryLastReplacedTimestampMs = 0,
                lastUpdatedTimestampMs = 0,
                imagePath = "",
            ),
            parsed,
        )
    }

    @Test
    fun `BatteryEventWire applies proto3 defaults`() {
        val parsed = lenient.decodeFromString<BatteryEventWire>("""{"id":"x"}""")
        assertEquals(
            BatteryEventWire(id = "x", deviceId = "", dateTimestampMs = 0, createdTimestampMs = 0, notes = ""),
            parsed,
        )
    }

    @Test
    fun `sync envelopes default their arrays to empty`() {
        assertEquals(SyncSnapshotWire(), lenient.decodeFromString<SyncSnapshotWire>("{}"))
        assertEquals(SyncPushRequestWire(), lenient.decodeFromString<SyncPushRequestWire>("{}"))
        assertEquals(SyncPushResponseWire(), lenient.decodeFromString<SyncPushResponseWire>("{}"))
    }

    // endregion

    // region leniency — an additive field from a newer peer must not break an older client

    @Test
    fun `unknown keys are ignored by the lenient consumer`() {
        val parsed = lenient.decodeFromString<DeviceWire>(
            """{"id":"x","name":"Alarm","someFutureField":42,"nested":{"a":1}}""",
        )
        assertEquals("x", parsed.id)
        assertEquals("Alarm", parsed.name)
    }

    @Test
    fun `a full round-trip preserves every field`() {
        val snapshot = SyncSnapshotWire(
            deviceTypes = listOf(DeviceTypeWire(id = "t1", name = "Smoke Alarm", defaultIcon = "smoke", batteryType = "9V", batteryQuantity = 2)),
            devices = listOf(
                DeviceWire(
                    id = "d1",
                    name = "Kitchen Alarm",
                    typeId = "t1",
                    location = "Kitchen",
                    batteryLastReplacedTimestampMs = 1_704_067_200_000,
                    lastUpdatedTimestampMs = 1_704_153_600_000,
                    imagePath = "/img/a.jpg",
                ),
            ),
            events = listOf(
                BatteryEventWire(id = "e1", deviceId = "d1", dateTimestampMs = 1_704_067_200_000, createdTimestampMs = 1_704_067_200_000, notes = "Duracell"),
            ),
        )
        val roundTripped = lenient.decodeFromString<SyncSnapshotWire>(canonical.encodeToString(snapshot))
        assertEquals(snapshot, roundTripped)
    }

    // endregion
}
