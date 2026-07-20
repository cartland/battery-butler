package com.chriscartland.batterybutler.presentationcore.util

import co.touchlab.kermit.Logger
import java.awt.Color
import java.awt.RenderingHints
import java.awt.image.BufferedImage
import java.io.ByteArrayInputStream
import java.io.ByteArrayOutputStream
import javax.imageio.IIOImage
import javax.imageio.ImageIO
import javax.imageio.ImageWriteParam
import kotlin.math.min

actual fun normalizeDeviceImage(bytes: ByteArray): NormalizedDeviceImage? {
    val decoded = try {
        ImageIO.read(ByteArrayInputStream(bytes))
    } catch (e: Exception) {
        Logger.w("DeviceImageNormalizer", e) { "Failed to decode picked image" }
        null
    } ?: return null

    val scaled = downscale(decoded, DEVICE_IMAGE_MAX_DIMENSION_PX)
    val flattened = flattenToOpaqueRgb(scaled)
    val jpegBytes = encodeJpeg(flattened, DEVICE_IMAGE_JPEG_QUALITY) ?: return null
    return NormalizedDeviceImage(bytes = jpegBytes, contentType = "image/jpeg")
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
        Logger.w("DeviceImageNormalizer", e) { "Failed to JPEG-encode the normalized image" }
        null
    } finally {
        writer.dispose()
    }
}
