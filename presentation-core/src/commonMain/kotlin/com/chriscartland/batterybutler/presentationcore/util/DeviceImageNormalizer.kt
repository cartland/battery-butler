package com.chriscartland.batterybutler.presentationcore.util

/** JPEG output of [normalizeDeviceImage]. */
class NormalizedDeviceImage(
    val bytes: ByteArray,
    val contentType: String,
)

/**
 * Normalizes a picked image for upload: decodes [bytes] (JPEG/PNG/HEIC/whatever the platform
 * picker returned), corrects for EXIF/orientation metadata, downscales so the long edge is at
 * most [DEVICE_IMAGE_MAX_DIMENSION_PX], and re-encodes as JPEG at [DEVICE_IMAGE_JPEG_QUALITY].
 * Downscaling also strips EXIF/GPS as a side effect. Returns null if [bytes] can't be decoded as
 * an image. See `docs/DEVICE_IMAGES.md` §6C -- the backend rejects HEIC and requires the
 * `Content-Type` header to match the actual bytes, so this must always emit real JPEG.
 */
expect fun normalizeDeviceImage(bytes: ByteArray): NormalizedDeviceImage?

/** Long edge target post-downscale (~2048px per the spec, well under the 10 MB upload cap). */
const val DEVICE_IMAGE_MAX_DIMENSION_PX = 2048

/** JPEG quality (0-100) used for the re-encode. */
const val DEVICE_IMAGE_JPEG_QUALITY = 80
