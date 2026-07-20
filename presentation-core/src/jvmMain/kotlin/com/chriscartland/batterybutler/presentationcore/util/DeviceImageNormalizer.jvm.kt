package com.chriscartland.batterybutler.presentationcore.util

import co.touchlab.kermit.Logger
import java.awt.Color
import java.awt.RenderingHints
import java.awt.geom.AffineTransform
import java.awt.image.AffineTransformOp
import java.awt.image.BufferedImage
import java.io.ByteArrayInputStream
import java.io.ByteArrayOutputStream
import javax.imageio.IIOImage
import javax.imageio.ImageIO
import javax.imageio.ImageWriteParam
import kotlin.math.min

private const val TAG = "DeviceImageNormalizer"

actual fun normalizeDeviceImage(bytes: ByteArray): NormalizedDeviceImage? {
    val decoded = try {
        ImageIO.read(ByteArrayInputStream(bytes))
    } catch (e: Exception) {
        Logger.w(TAG, e) { "Failed to decode picked image" }
        null
    } ?: return null

    val oriented = applyExifOrientation(decoded, readJpegExifOrientation(bytes))
    val scaled = downscale(oriented, DEVICE_IMAGE_MAX_DIMENSION_PX)
    val flattened = flattenToOpaqueRgb(scaled)
    val jpegBytes = encodeJpeg(flattened, DEVICE_IMAGE_JPEG_QUALITY) ?: return null
    return NormalizedDeviceImage(bytes = jpegBytes, contentType = "image/jpeg")
}

/**
 * `ImageIO.read` ignores EXIF orientation (it decodes raw physical pixels) -- rotate/flip [image]
 * to match. Values follow the standard EXIF orientation tag (1-8); 1 (or anything unrecognized)
 * means no transform needed.
 */
private fun applyExifOrientation(
    image: BufferedImage,
    orientation: Int,
): BufferedImage {
    if (orientation <= 1) return image

    val w = image.width.toDouble()
    val h = image.height.toDouble()
    val transform = AffineTransform()
    val swapsDimensions: Boolean
    when (orientation) {
        2 -> { // flip horizontal
            transform.scale(-1.0, 1.0)
            transform.translate(-w, 0.0)
            swapsDimensions = false
        }

        3 -> { // rotate 180
            transform.translate(w, h)
            transform.rotate(Math.PI)
            swapsDimensions = false
        }

        4 -> { // flip vertical
            transform.scale(1.0, -1.0)
            transform.translate(0.0, -h)
            swapsDimensions = false
        }

        5 -> { // transpose (rotate -90 then flip horizontal)
            transform.rotate(-Math.PI / 2)
            transform.scale(-1.0, 1.0)
            swapsDimensions = true
        }

        6 -> { // rotate 90
            transform.translate(h, 0.0)
            transform.rotate(Math.PI / 2)
            swapsDimensions = true
        }

        7 -> { // transverse (mirror horizontal, then rotate 90 CW)
            transform.translate(h, w)
            transform.scale(1.0, -1.0)
            transform.rotate(Math.PI / 2)
            swapsDimensions = true
        }

        8 -> { // rotate 270
            transform.translate(0.0, w)
            transform.rotate(3 * Math.PI / 2)
            swapsDimensions = true
        }

        else -> {
            return image
        }
    }

    val destWidth = if (swapsDimensions) image.height else image.width
    val destHeight = if (swapsDimensions) image.width else image.height
    val destination = BufferedImage(destWidth, destHeight, BufferedImage.TYPE_INT_ARGB)
    val g = destination.createGraphics()
    try {
        g.setRenderingHint(RenderingHints.KEY_INTERPOLATION, RenderingHints.VALUE_INTERPOLATION_BILINEAR)
        g.drawImage(image, AffineTransformOp(transform, AffineTransformOp.TYPE_BILINEAR), 0, 0)
    } finally {
        g.dispose()
    }
    return destination
}

/**
 * Manually scans [bytes] for a JPEG `APP1`/Exif segment and reads the orientation tag (0x0112),
 * without a new dependency -- `javax.imageio`'s own metadata API doesn't expose this cleanly.
 * Returns 1 (normal, no-op) for anything that isn't a baseline JPEG with a readable EXIF
 * orientation tag (PNG/WebP/etc have no EXIF concept here, so this is JPEG-only by design).
 */
private fun readJpegExifOrientation(bytes: ByteArray): Int {
    try {
        if (bytes.size < 4 || bytes[0] != 0xFF.toByte() || bytes[1] != 0xD8.toByte()) return 1

        var offset = 2
        while (offset + 4 <= bytes.size) {
            if (bytes[offset] != 0xFF.toByte()) return 1
            val marker = bytes[offset + 1].toInt() and 0xFF
            if (marker == 0xD8 || marker == 0xD9) {
                offset += 2
                continue
            }
            val segmentLength = ((bytes[offset + 2].toInt() and 0xFF) shl 8) or (bytes[offset + 3].toInt() and 0xFF)
            if (marker == 0xE1) {
                val orientation = readOrientationFromApp1(bytes, offset + 4, segmentLength - 2)
                if (orientation != null) return orientation
            }
            if (marker == 0xDA) return 1 // start of scan -- no more markers to look for
            offset += 2 + segmentLength
        }
    } catch (e: Exception) {
        Logger.w(TAG, e) { "Failed to read EXIF orientation; assuming normal" }
    }
    return 1
}

private fun readOrientationFromApp1(
    bytes: ByteArray,
    app1Start: Int,
    app1Length: Int,
): Int? {
    if (app1Length < 8 || app1Start + 6 > bytes.size) return null
    val exifHeader = String(bytes, app1Start, 6, Charsets.US_ASCII)
    if (exifHeader != "Exif\u0000\u0000") return null

    val tiffStart = app1Start + 6
    if (tiffStart + 8 > bytes.size) return null
    val littleEndian = bytes[tiffStart] == 'I'.code.toByte() && bytes[tiffStart + 1] == 'I'.code.toByte()
    val bigEndian = bytes[tiffStart] == 'M'.code.toByte() && bytes[tiffStart + 1] == 'M'.code.toByte()
    if (!littleEndian && !bigEndian) return null

    fun readU16(pos: Int): Int =
        if (littleEndian) {
            (bytes[pos].toInt() and 0xFF) or ((bytes[pos + 1].toInt() and 0xFF) shl 8)
        } else {
            ((bytes[pos].toInt() and 0xFF) shl 8) or (bytes[pos + 1].toInt() and 0xFF)
        }

    fun readU32(pos: Int): Int =
        if (littleEndian) {
            (bytes[pos].toInt() and 0xFF) or ((bytes[pos + 1].toInt() and 0xFF) shl 8) or
                ((bytes[pos + 2].toInt() and 0xFF) shl 16) or ((bytes[pos + 3].toInt() and 0xFF) shl 24)
        } else {
            ((bytes[pos].toInt() and 0xFF) shl 24) or ((bytes[pos + 1].toInt() and 0xFF) shl 16) or
                ((bytes[pos + 2].toInt() and 0xFF) shl 8) or (bytes[pos + 3].toInt() and 0xFF)
        }

    val ifd0Offset = tiffStart + readU32(tiffStart + 4)
    if (ifd0Offset + 2 > bytes.size) return null
    val entryCount = readU16(ifd0Offset)
    for (i in 0 until entryCount) {
        val entryStart = ifd0Offset + 2 + (i * 12)
        if (entryStart + 12 > bytes.size) break
        val tag = readU16(entryStart)
        if (tag == 0x0112) {
            val value = readU16(entryStart + 8)
            return if (value in 1..8) value else null
        }
    }
    return null
}

/** Scales [image] down so its long edge is at most [maxDimensionPx]; never upscales. */
private fun downscale(
    image: BufferedImage,
    maxDimensionPx: Int,
): BufferedImage {
    val longEdge = maxOf(image.width, image.height)
    if (longEdge <= maxDimensionPx) return image

    val scale = maxDimensionPx.toDouble() / longEdge
    val targetWidth = maxOf(1, (image.width * scale).toInt())
    val targetHeight = maxOf(1, (image.height * scale).toInt())

    val scaled = BufferedImage(targetWidth, targetHeight, BufferedImage.TYPE_INT_ARGB)
    val g = scaled.createGraphics()
    try {
        g.setRenderingHint(RenderingHints.KEY_INTERPOLATION, RenderingHints.VALUE_INTERPOLATION_BILINEAR)
        g.setRenderingHint(RenderingHints.KEY_RENDERING, RenderingHints.VALUE_RENDER_QUALITY)
        g.drawImage(image, 0, 0, targetWidth, targetHeight, null)
    } finally {
        g.dispose()
    }
    return scaled
}

/** JPEG has no alpha channel -- flatten any transparency onto white before encoding. */
private fun flattenToOpaqueRgb(image: BufferedImage): BufferedImage {
    if (image.type == BufferedImage.TYPE_INT_RGB && !image.colorModel.hasAlpha()) return image
    val opaque = BufferedImage(image.width, image.height, BufferedImage.TYPE_INT_RGB)
    val g = opaque.createGraphics()
    try {
        g.color = Color.WHITE
        g.fillRect(0, 0, image.width, image.height)
        g.drawImage(image, 0, 0, null)
    } finally {
        g.dispose()
    }
    return opaque
}

private fun encodeJpeg(
    image: BufferedImage,
    qualityPercent: Int,
): ByteArray? {
    val writers = ImageIO.getImageWritersByFormatName("jpg")
    if (!writers.hasNext()) return null
    val writer = writers.next()
    val output = ByteArrayOutputStream()
    return try {
        ImageIO.createImageOutputStream(output).use { ios ->
            writer.output = ios
            val params = writer.defaultWriteParam.apply {
                compressionMode = ImageWriteParam.MODE_EXPLICIT
                compressionQuality = min(qualityPercent, 100) / 100f
            }
            writer.write(null, IIOImage(image, null, null), params)
        }
        output.toByteArray()
    } catch (e: Exception) {
        Logger.w(TAG, e) { "Failed to JPEG-encode the normalized image" }
        null
    } finally {
        writer.dispose()
    }
}
