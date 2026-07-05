package com.chriscartland.batterybutler.presentationcore.util

import android.app.Activity
import android.content.ClipData
import android.content.ClipDescription
import android.content.ClipboardManager
import android.content.Context
import android.os.Build
import android.os.PersistableBundle
import java.lang.ref.WeakReference

class AndroidSecureClipboard(
    activity: Activity,
) : SecureClipboard {
    private val activityRef = WeakReference(activity)

    override fun copySensitive(text: String) {
        val activity = activityRef.get() ?: return
        val clipboardManager =
            activity.getSystemService(Context.CLIPBOARD_SERVICE) as? ClipboardManager ?: return
        val clip = ClipData.newPlainText(LABEL, text)
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU) {
            clip.description.extras = PersistableBundle().apply {
                putBoolean(ClipDescription.EXTRA_IS_SENSITIVE, true)
            }
        }
        clipboardManager.setPrimaryClip(clip)
    }

    private companion object {
        const val LABEL = "Battery Butler credential"
    }
}
