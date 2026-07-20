package com.chriscartland.batterybutler.presentationcore.util

import android.app.Activity
import android.net.Uri
import androidx.activity.result.ActivityResultLauncher
import androidx.activity.result.PickVisualMediaRequest
import androidx.activity.result.contract.ActivityResultContracts
import co.touchlab.kermit.Logger
import java.lang.ref.WeakReference

private const val TAG = "AndroidDeviceImagePicker"

/**
 * Uses the Android Photo Picker (`ActivityResultContracts.PickVisualMedia`) -- no storage
 * permission needed. See `docs/DEVICE_IMAGES.md` §6C.
 */
class AndroidDeviceImagePicker(
    activity: Activity,
    private val pickImageLauncher: ActivityResultLauncher<PickVisualMediaRequest>,
) : DeviceImagePicker {
    private val activityRef = WeakReference(activity)
    private var pendingCallback: ((ByteArray?) -> Unit)? = null

    override fun pickImage(onResult: (ByteArray?) -> Unit) {
        pendingCallback = onResult
        pickImageLauncher.launch(
            PickVisualMediaRequest(ActivityResultContracts.PickVisualMedia.ImageOnly),
        )
    }

    fun handleResult(uri: Uri?) {
        val callback = pendingCallback
        pendingCallback = null
        if (uri == null) {
            callback?.invoke(null)
            return
        }
        val activity = activityRef.get()
        if (activity == null) {
            callback?.invoke(null)
            return
        }
        val bytes = try {
            activity.contentResolver.openInputStream(uri)?.use { it.readBytes() }
        } catch (e: Exception) {
            Logger.w(TAG, e) { "Failed to read picked image bytes" }
            null
        }
        callback?.invoke(bytes)
    }
}
