package com.chriscartland.batterybutler.presentationcore.components

import androidx.compose.runtime.Composable
import androidx.compose.runtime.produceState
import androidx.compose.ui.graphics.ImageBitmap
import com.chriscartland.batterybutler.domain.model.DeviceImageBytes

/**
 * Returns the decoded [ImageBitmap] for [imageBytes], or null if there's nothing to show yet (no
 * photo, not cached yet) or the bytes are corrupt. Decoding runs off the composition thread via
 * [loader] -- see [DeviceImageBitmapLoader] for why the cache is keyed by [imageEtag] rather than
 * the byte array itself.
 */
@Composable
fun rememberDeviceImageBitmap(
    imageEtag: String?,
    imageBytes: DeviceImageBytes?,
    loader: DeviceImageBitmapLoader = DeviceImageBitmapLoader.shared,
): ImageBitmap? {
    val cachedBitmap = imageEtag?.let { loader.peek(it) }
    val bitmapState = produceState(initialValue = cachedBitmap, imageEtag, imageBytes, loader) {
        value = if (imageEtag != null && imageBytes != null) {
            loader.load(imageEtag, imageBytes.bytes)
        } else {
            null
        }
    }
    return bitmapState.value
}
