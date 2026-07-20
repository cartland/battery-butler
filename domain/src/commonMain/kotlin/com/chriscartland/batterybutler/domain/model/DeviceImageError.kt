package com.chriscartland.batterybutler.domain.model

/**
 * Errors from the Labs backend's device-image endpoints (`PUT/GET/DELETE
 * .../devices/{id}/image`). See `docs/DEVICE_IMAGES.md` §4b for the full contract.
 */
sealed interface DeviceImageError : AppError {
    /** HTTP 400 -- the content type didn't match the actual bytes, or the image is malformed/unsupported. */
    data class InvalidImage(
        override val message: String = "Unsupported image type or corrupt image data",
        override val cause: String? = null,
    ) : DeviceImageError

    /** HTTP 404 on upload -- the device isn't synced to the backend yet. */
    data class DeviceNotFound(
        override val message: String = "Device not found -- sync it before uploading a photo",
        override val cause: String? = null,
    ) : DeviceImageError

    /** HTTP 413 -- the image exceeds the backend's 10 MB cap. */
    data class TooLarge(
        override val message: String = "Image is too large (10 MB limit)",
        override val cause: String? = null,
    ) : DeviceImageError

    /** Transport failure (unreachable server, timeout) or an unexpected HTTP status. */
    data class NetworkError(
        override val message: String = "Network error",
        override val cause: String? = null,
    ) : DeviceImageError
}
