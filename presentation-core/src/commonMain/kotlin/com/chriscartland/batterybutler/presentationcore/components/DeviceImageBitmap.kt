package com.chriscartland.batterybutler.presentationcore.components

import androidx.compose.runtime.Composable
import androidx.compose.runtime.remember
import androidx.compose.ui.graphics.ImageBitmap
import co.touchlab.kermit.Logger
import com.chriscartland.batterybutler.domain.model.DeviceImageBytes
import org.jetbrains.compose.resources.decodeToImageBitmap

/**
 * Decodes [imageBytes] to an [ImageBitmap] for display, or null if there's nothing to show yet
 * (no photo, not cached yet) or the bytes are corrupt. Decode is a manual, one-off call --
 * [org.jetbrains.compose.resources.decodeToImageBitmap] is the same Skia-backed decoder Compose
 * Multiplatform already uses for bundled resources, so this needs no new dependency (the
 * recommended choice in `docs/DEVICE_IMAGES.md` §6D/§8.1). [remember]ed by the byte array
 * reference, so a given cached photo is decoded once per composition, not on every recomposition.
 */
@Composable
fun rememberDeviceImageBitmap(imageBytes: DeviceImageBytes?): ImageBitmap? =
    remember(imageBytes?.bytes) {
        imageBytes?.bytes?.let {
            try {
                it.decodeToImageBitmap()
            } catch (e: Exception) {
                Logger.w("DeviceImageBitmap", e) { "Failed to decode cached device image" }
                null
            }
        }
    }
