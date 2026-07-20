package com.chriscartland.batterybutler.presentationcore.util

import kotlinx.cinterop.ExperimentalForeignApi
import kotlinx.cinterop.addressOf
import kotlinx.cinterop.useContents
import kotlinx.cinterop.usePinned
import platform.CoreGraphics.CGRectMake
import platform.CoreGraphics.CGSizeMake
import platform.Foundation.NSData
import platform.Foundation.dataWithBytes
import platform.UIKit.UIColor
import platform.UIKit.UIGraphicsBeginImageContextWithOptions
import platform.UIKit.UIGraphicsEndImageContext
import platform.UIKit.UIGraphicsGetImageFromCurrentImageContext
import platform.UIKit.UIImage
import platform.UIKit.UIImageJPEGRepresentation
import platform.UIKit.UIRectFill
import platform.posix.memcpy

/**
 * `UIImage(data:)` decodes JPEG/PNG/HEIC alike and carries EXIF orientation in
 * [UIImage.imageOrientation] rather than baking it into pixels -- redrawing through a UIKit image
 * context (as [downscale] does, for every input, even one already under the max dimension) both
 * applies that orientation correctly AND re-encodes as JPEG, so HEIC input needs no separate
 * conversion step. See `docs/DEVICE_IMAGES.md` §6C/§7.2.
 */
@OptIn(ExperimentalForeignApi::class)
actual fun normalizeDeviceImage(bytes: ByteArray): NormalizedDeviceImage? {
    if (bytes.isEmpty()) return null
    val image = UIImage(data = bytes.toNSData())
    // UIImage(data:) doesn't surface undecodable input as null in this binding -- a zero-size
    // image is what corrupt/unrecognized bytes produce instead.
    val (width, height) = image.size.useContents { width to height }
    if (width <= 0.0 || height <= 0.0) return null

    val redrawn = downscale(image, DEVICE_IMAGE_MAX_DIMENSION_PX.toDouble())
    val jpegData = UIImageJPEGRepresentation(redrawn, DEVICE_IMAGE_JPEG_QUALITY / 100.0) ?: return null
    return NormalizedDeviceImage(bytes = jpegData.toByteArray(), contentType = "image/jpeg")
}

/**
 * Redraws [image] into a new opaque (white-backed, so PNG transparency flattens instead of
 * rendering black in JPEG) bitmap context, downscaled so the long edge is at most
 * [maxDimensionPx] -- never upscaled. This redraw is also what bakes EXIF orientation into pixels
 * (see class doc), so it always runs, even when no resize is needed.
 */
@OptIn(ExperimentalForeignApi::class)
private fun downscale(
    image: UIImage,
    maxDimensionPx: Double,
): UIImage {
    val (width, height) = image.size.useContents { width to height }
    val longEdge = maxOf(width, height)
    val scale = if (longEdge > maxDimensionPx) maxDimensionPx / longEdge else 1.0
    val targetWidth = width * scale
    val targetHeight = height * scale
    val rect = CGRectMake(0.0, 0.0, targetWidth, targetHeight)

    UIGraphicsBeginImageContextWithOptions(CGSizeMake(targetWidth, targetHeight), false, 1.0)
    return try {
        UIColor.whiteColor.setFill()
        UIRectFill(rect)
        image.drawInRect(rect)
        UIGraphicsGetImageFromCurrentImageContext() ?: image
    } finally {
        UIGraphicsEndImageContext()
    }
}

@OptIn(ExperimentalForeignApi::class)
internal fun ByteArray.toNSData(): NSData {
    if (isEmpty()) return NSData()
    return usePinned { pinned -> NSData.dataWithBytes(pinned.addressOf(0), size.toULong()) }
}

@OptIn(ExperimentalForeignApi::class)
internal fun NSData.toByteArray(): ByteArray {
    val size = length.toInt()
    if (size == 0) return ByteArray(0)
    val source = bytes ?: return ByteArray(0)
    val result = ByteArray(size)
    result.usePinned { pinned ->
        memcpy(pinned.addressOf(0), source, length)
    }
    return result
}
