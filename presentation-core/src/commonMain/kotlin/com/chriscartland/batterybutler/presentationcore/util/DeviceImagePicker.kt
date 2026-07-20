package com.chriscartland.batterybutler.presentationcore.util

import androidx.compose.runtime.compositionLocalOf

/**
 * Picks an image from the user's photo library/files. [onResult] receives the picked bytes in
 * whatever format the OS/user provided (JPEG/PNG/HEIC/etc -- not yet normalized for upload), or
 * null if the user cancelled or the pick failed. See `docs/DEVICE_IMAGES.md` §6C.
 */
fun interface DeviceImagePicker {
    fun pickImage(onResult: (ByteArray?) -> Unit)
}

val LocalDeviceImagePicker = compositionLocalOf<DeviceImagePicker> {
    error("No DeviceImagePicker provided")
}
