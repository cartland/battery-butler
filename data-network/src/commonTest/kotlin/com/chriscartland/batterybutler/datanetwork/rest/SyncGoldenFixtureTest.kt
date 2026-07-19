package com.chriscartland.batterybutler.datanetwork.rest

import kotlinx.serialization.encodeToString
import kotlinx.serialization.json.Json
import kotlin.test.Test
import kotlin.test.assertEquals

/**
 * Golden-fixture cross-check. The JSON below is byte-for-byte the SAME canonical wire example
 * the Labs backend pins on its side (the `.golden.json` files under
 * `services/labs-backend/test/fixtures/battery-butler/`).
 *
 * labs asserts "the producer emits exactly this canonical shape"; this test asserts "the client
 * parses exactly this into fully-populated DTOs with these values, and re-emits the same shape".
 * Composed, the two verify the client parses what labs emits — WITHOUT a live call — catching a
 * transcription mismatch the per-side field pins in [SyncWireContractTest] cannot (both sides can
 * be green while disagreeing). The remaining cross-repo *drift* guard is the live integration test
 * (a follow-up). **EDIT IN LOCKSTEP with the labs fixtures.**
 */
class SyncGoldenFixtureTest {
    private val canonical = Json { encodeDefaults = true }
    private val lenient = Json { ignoreUnknownKeys = true }

    @Test
    fun `snapshot golden parses into fully-populated DTOs (GET sync)`() {
        val snapshot = lenient.decodeFromString<SyncSnapshotWire>(SNAPSHOT_GOLDEN)

        assertEquals(
            DeviceTypeWire(
                id = "type-smoke",
                name = "Smoke Alarm",
                defaultIcon = "detector_smoke",
                batteryType = "9V",
                batteryQuantity = 2,
            ),
            snapshot.deviceTypes.single(),
        )
        assertEquals(
            DeviceSnapshotWire(
                id = "device-kitchen",
                name = "Kitchen Alarm",
                typeId = "type-smoke",
                location = "Kitchen",
                batteryLastReplacedTimestampMs = 1_704_067_200_000,
                lastUpdatedTimestampMs = 1_704_153_600_000,
                imagePath = "/images/kitchen-alarm.jpg",
                imageEtag = "e3b0c44298fc1c149afbf4c8996fb92427ae41e4649b934ca495991b7852b855",
            ),
            snapshot.devices.single(),
        )
        assertEquals(
            BatteryEventWire(
                id = "event-jan",
                deviceId = "device-kitchen",
                dateTimestampMs = 1_704_067_200_000,
                createdTimestampMs = 1_704_067_300_000,
                notes = "Replaced with Duracell",
            ),
            snapshot.events.single(),
        )

        // Re-emit must reproduce the same canonical tree the backend pinned (order-insensitive).
        assertEquals(
            lenient.parseToJsonElement(SNAPSHOT_GOLDEN),
            lenient.parseToJsonElement(canonical.encodeToString(snapshot)),
        )
    }

    @Test
    fun `push-request golden parses into fully-populated DTOs (POST sync)`() {
        val push = lenient.decodeFromString<SyncPushRequestWire>(PUSH_REQUEST_GOLDEN)

        assertEquals(listOf("type-old"), push.deletedDeviceTypeIds)
        assertEquals(listOf("device-removed"), push.deletedDeviceIds)
        assertEquals(listOf("event-removed"), push.deletedEventIds)
        assertEquals("type-smoke", push.devices.single().typeId)
        assertEquals("/images/kitchen-alarm.jpg", push.devices.single().imagePath)

        assertEquals(
            lenient.parseToJsonElement(PUSH_REQUEST_GOLDEN),
            lenient.parseToJsonElement(canonical.encodeToString(push)),
        )
    }

    private companion object {
        // Byte-shared with: services/labs-backend/test/fixtures/battery-butler/sync-snapshot.golden.json
        val SNAPSHOT_GOLDEN =
            """
            {
              "deviceTypes": [
                {
                  "id": "type-smoke",
                  "name": "Smoke Alarm",
                  "defaultIcon": "detector_smoke",
                  "batteryType": "9V",
                  "batteryQuantity": 2
                }
              ],
              "devices": [
                {
                  "id": "device-kitchen",
                  "name": "Kitchen Alarm",
                  "typeId": "type-smoke",
                  "location": "Kitchen",
                  "batteryLastReplacedTimestampMs": 1704067200000,
                  "lastUpdatedTimestampMs": 1704153600000,
                  "imagePath": "/images/kitchen-alarm.jpg",
                  "imageEtag": "e3b0c44298fc1c149afbf4c8996fb92427ae41e4649b934ca495991b7852b855"
                }
              ],
              "events": [
                {
                  "id": "event-jan",
                  "deviceId": "device-kitchen",
                  "dateTimestampMs": 1704067200000,
                  "createdTimestampMs": 1704067300000,
                  "notes": "Replaced with Duracell"
                }
              ]
            }
            """.trimIndent()

        // Byte-shared with: services/labs-backend/test/fixtures/battery-butler/sync-push-request.golden.json
        val PUSH_REQUEST_GOLDEN =
            """
            {
              "deviceTypes": [
                {
                  "id": "type-smoke",
                  "name": "Smoke Alarm",
                  "defaultIcon": "detector_smoke",
                  "batteryType": "9V",
                  "batteryQuantity": 2
                }
              ],
              "devices": [
                {
                  "id": "device-kitchen",
                  "name": "Kitchen Alarm",
                  "typeId": "type-smoke",
                  "location": "Kitchen",
                  "batteryLastReplacedTimestampMs": 1704067200000,
                  "lastUpdatedTimestampMs": 1704153600000,
                  "imagePath": "/images/kitchen-alarm.jpg"
                }
              ],
              "events": [
                {
                  "id": "event-jan",
                  "deviceId": "device-kitchen",
                  "dateTimestampMs": 1704067200000,
                  "createdTimestampMs": 1704067300000,
                  "notes": "Replaced with Duracell"
                }
              ],
              "deletedDeviceTypeIds": ["type-old"],
              "deletedDeviceIds": ["device-removed"],
              "deletedEventIds": ["event-removed"]
            }
            """.trimIndent()
    }
}
