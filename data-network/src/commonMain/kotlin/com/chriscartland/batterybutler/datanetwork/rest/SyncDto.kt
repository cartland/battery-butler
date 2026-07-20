package com.chriscartland.batterybutler.datanetwork.rest

import kotlinx.serialization.Serializable

/*
 * REST sync wire DTOs — the JSON contract between this client and the Labs backend's
 * `/v1/battery-butler/sync` endpoint.
 *
 * These mirror the backend's wire schema (its Zod `syncSchema.ts`), which in turn mirrors
 * this app's `protos/.../model.proto`. Cross-repo source imports are impossible, so the
 * contract is *re-declared* on each side and pinned by a test on each side
 * ([SyncWireContractTest] here; `batteryButlerWireContract.test.ts` on the backend). A
 * rename, type change, or field-set change must edit BOTH sides — a deliberate, reviewed act.
 *
 * Defensive boundary: a mobile install can't be force-updated, so old clients keep calling.
 * The producer emits the canonical shape (every field present, proto3 defaults applied) and
 * the consumer parses leniently — every field carries its proto3 default so a field the peer
 * omits never breaks parsing, and unknown keys are ignored by the `Json` used to decode.
 * Adding a field with a default here is therefore backward-compatible.
 *
 * The `id` is the document id; it is required (no default). Every other field mirrors the
 * proto with its proto3 zero-value default. Timestamps are epoch milliseconds.
 */

/** A category of devices (e.g. "Smoke Alarm") with default battery characteristics. */
@Serializable
data class DeviceTypeWire(
    val id: String,
    val name: String = "",
    val defaultIcon: String = "",
    val batteryType: String = "AA",
    val batteryQuantity: Int = 1,
)

/** A specific device instance that uses batteries. */
@Serializable
data class DeviceWire(
    val id: String,
    val name: String = "",
    val typeId: String = "",
    val location: String = "",
    val batteryLastReplacedTimestampMs: Long = 0,
    val lastUpdatedTimestampMs: Long = 0,
    val imagePath: String = "",
)

/**
 * A device as it appears in a `GET /sync` snapshot: [DeviceWire]'s fields plus the
 * server-managed [imageEtag]. Snapshot-only -- `POST /sync` still carries plain [DeviceWire] --
 * because a push replaces the whole device record server-side, so server-owned state can't live
 * in the same shape a client pushes. See `docs/DEVICE_IMAGES.md` §3.
 */
@Serializable
data class DeviceSnapshotWire(
    val id: String,
    val name: String = "",
    val typeId: String = "",
    val location: String = "",
    val batteryLastReplacedTimestampMs: Long = 0,
    val lastUpdatedTimestampMs: Long = 0,
    val imagePath: String = "",
    val imageEtag: String = "",
)

/** A battery-replacement event for a device. */
@Serializable
data class BatteryEventWire(
    val id: String,
    val deviceId: String = "",
    val dateTimestampMs: Long = 0,
    val createdTimestampMs: Long = 0,
    val notes: String = "",
)

/** `GET /sync` response — a full snapshot of all three collections. */
@Serializable
data class SyncSnapshotWire(
    val deviceTypes: List<DeviceTypeWire> = emptyList(),
    val devices: List<DeviceSnapshotWire> = emptyList(),
    val events: List<BatteryEventWire> = emptyList(),
)

/**
 * `POST /sync` request — a patch that upserts the listed entities and removes the listed ids.
 * All arrays default to empty, so a partial push (e.g. devices only) is valid. The proto's
 * `isFullSnapshot` is intentionally not modeled: every push is an upsert/delete patch.
 */
@Serializable
data class SyncPushRequestWire(
    val deviceTypes: List<DeviceTypeWire> = emptyList(),
    val devices: List<DeviceWire> = emptyList(),
    val events: List<BatteryEventWire> = emptyList(),
    val deletedDeviceTypeIds: List<String> = emptyList(),
    val deletedDeviceIds: List<String> = emptyList(),
    val deletedEventIds: List<String> = emptyList(),
)

/** `POST /sync` response. */
@Serializable
data class SyncPushResponseWire(
    val success: Boolean = false,
    val message: String = "",
)
