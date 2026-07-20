package com.chriscartland.batterybutler.domain.model

/**
 * Raw image bytes with the `Content-Type` they were served/stored as. Shared by `data-network`
 * (fetched from the Labs backend) and `data-local` (cached on-device, keyed by `imageEtag`) --
 * defined here so neither module needs to depend on the other. Deliberately not a `data class`:
 * [ByteArray] equality is reference-based, so a generated `equals`/`hashCode` would be
 * misleading; compare `.bytes.contentEquals(...)` directly where needed.
 */
class DeviceImageBytes(
    val bytes: ByteArray,
    val contentType: String,
)
