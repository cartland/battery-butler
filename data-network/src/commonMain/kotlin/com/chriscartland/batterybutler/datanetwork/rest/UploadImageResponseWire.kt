package com.chriscartland.batterybutler.datanetwork.rest

import kotlinx.serialization.Serializable

/** Response from `PUT .../devices/{id}/image`. */
@Serializable
internal data class UploadImageResponseWire(
    val imageEtag: String = "",
)
