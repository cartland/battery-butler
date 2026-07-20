package com.chriscartland.batterybutler.presentationcore.util

import android.graphics.Bitmap
import android.graphics.BitmapFactory
import android.graphics.Canvas
import android.graphics.Color
import android.graphics.Matrix
import androidx.exifinterface.media.ExifInterface
import co.touchlab.kermit.Logger
import java.io.ByteArrayInputStream
import java.io.ByteArrayOutputStream

private const val TAG = "DeviceImageNormalizer"

actual fun normalizeDeviceImage(bytes: ByteArray): NormalizedDeviceImage? {
    val decoded = try {
        BitmapFactory.decodeByteArray(bytes, 0, bytes.size)
    } catch (e: Exception) {
        Logger.w(TAG, e) { "Failed to decode picked image" }
        null
    } ?: return null

    val oriented = applyExifOrientation(bytes, decoded)
    val scaled = downscale(oriented, DEVICE_IMAGE_MAX_DIMENSION_PX)
    val flattened = flattenToOpaqueRgb(scaled)

    val output = ByteArrayOutputStream()
    val wasCompressed = flattened.compress(Bitmap.CompressFormat.JPEG, DEVICE_IMAGE_JPEG_QUALITY, output)
    if (!wasCompressed) return null
    return NormalizedDeviceImage(bytes = output.toByteArray(), contentType = "image/jpeg")
}

/** `BitmapFactory` ignores EXIF orientation -- rotate/flip [bitmap] to match [rawBytes]'s tag. */
private fun applyExifOrientation(
    rawBytes: ByteArray,
    bitmap: Bitmap,
): Bitmap {
    val orientation = try {
        ExifInterface(ByteArrayInputStream(rawBytes)).getAttributeInt(
            ExifInterface.TAG_ORIENTATION,
            ExifInterface.ORIENTATION_NORMAL,
        )
    } catch (e: Exception) {
        Logger.w(TAG, e) { "Failed to read EXIF orientation; assuming normal" }
        ExifInterface.ORIENTATION_NORMAL
    }
    if (orientation == ExifInterface.ORIENTATION_NORMAL || orientation == ExifInterface.ORIENTATION_UNDEFINED) return bitmap

    val matrix = Matrix()
    when (orientation) {
        ExifInterface.ORIENTATION_ROTATE_90 -> {
            matrix.postRotate(90f)
        }

        ExifInterface.ORIENTATION_ROTATE_180 -> {
            matrix.postRotate(180f)
        }

        ExifInterface.ORIENTATION_ROTATE_270 -> {
            matrix.postRotate(270f)
        }

        ExifInterface.ORIENTATION_FLIP_HORIZONTAL -> {
            matrix.postScale(-1f, 1f)
        }

        ExifInterface.ORIENTATION_FLIP_VERTICAL -> {
            matrix.postScale(1f, -1f)
        }

        ExifInterface.ORIENTATION_TRANSPOSE -> {
            matrix.postRotate(90f)
            matrix.postScale(-1f, 1f)
        }

        ExifInterface.ORIENTATION_TRANSVERSE -> {
            matrix.postRotate(270f)
            matrix.postScale(-1f, 1f)
        }

        else -> {
            return bitmap
        }
    }
    return Bitmap.createBitmap(bitmap, 0, 0, bitmap.width, bitmap.height, matrix, true)
}

/** Scales [bitmap] down so its long edge is at most [maxDimensionPx]; never upscales. */
private fun downscale(
    bitmap: Bitmap,
    maxDimensionPx: Int,
): Bitmap {
    val longEdge = maxOf(bitmap.width, bitmap.height)
    if (longEdge <= maxDimensionPx) return bitmap

    val scale = maxDimensionPx.toDouble() / longEdge
    val targetWidth = maxOf(1, (bitmap.width * scale).toInt())
    val targetHeight = maxOf(1, (bitmap.height * scale).toInt())
    return Bitmap.createScaledBitmap(bitmap, targetWidth, targetHeight, true)
}

/** JPEG has no alpha channel -- flatten any transparency onto white before encoding. */
private fun flattenToOpaqueRgb(bitmap: Bitmap): Bitmap {
    if (!bitmap.hasAlpha()) return bitmap
    val opaque = Bitmap.createBitmap(bitmap.width, bitmap.height, Bitmap.Config.ARGB_8888)
    val canvas = Canvas(opaque)
    canvas.drawColor(Color.WHITE)
    canvas.drawBitmap(bitmap, 0f, 0f, null)
    return opaque
}
