package com.chriscartland.batterybutler.presentationcore.util

import android.app.Activity
import android.net.Uri
import androidx.activity.result.ActivityResultLauncher
import java.lang.ref.WeakReference

class AndroidFileLoader(
    activity: Activity,
    private val openDocumentLauncher: ActivityResultLauncher<Array<String>>,
) : FileLoader {
    private val activityRef = WeakReference(activity)
    private var pendingCallback: ((ByteArray?) -> Unit)? = null

    override fun loadFile(onResult: (ByteArray?) -> Unit) {
        pendingCallback = onResult
        openDocumentLauncher.launch(arrayOf("application/json"))
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
        val bytes = activity.contentResolver.openInputStream(uri)?.use { it.readBytes() }
        callback?.invoke(bytes)
    }
}
