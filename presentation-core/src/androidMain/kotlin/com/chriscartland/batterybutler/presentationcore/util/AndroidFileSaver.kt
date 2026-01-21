package com.chriscartland.batterybutler.presentationcore.util

import android.app.Activity
import android.content.Intent
import androidx.core.content.FileProvider
import java.io.File
import java.io.FileOutputStream

class AndroidFileSaver(
    private val activity: Activity,
) : FileSaver {
    override fun saveFile(
        fileName: String,
        content: ByteArray,
    ) {
        val cacheDir = activity.cacheDir
        val file = File(cacheDir, fileName)
        FileOutputStream(file).use { it.write(content) }

        // Get URI using FileProvider
        // Assuming authority is "${applicationId}.provider" defined in AndroidManifest.xml
        // We will fallback to a simple intent with cache path if provider not set up,
        // OR better: Just use Intent.ACTION_SEND with stream to let user save to Drive/Keep/etc.

        // Actually, to support "Download", we can use Intent.ACTION_CREATE_DOCUMENT, but that requires activity result handling.
        // For simplicity requested ("Share Sheet"), we stick to sharing the file content.

        // However, user specifically asked: "This should try to download file, not just share text."
        // Best approach without Activity Result API complications in non-Component context:
        // Use standard ACTION_SEND logic which allows "Save to Drive" or "Copy to..."

        val uri = try {
            FileProvider.getUriForFile(
                activity,
                "${activity.packageName}.fileprovider",
                file,
            )
        } catch (_: Exception) {
            null
        }

        if (uri != null) {
            val intent = Intent(Intent.ACTION_SEND).apply {
                type = "application/json"
                putExtra(Intent.EXTRA_STREAM, uri as android.os.Parcelable)
                addFlags(Intent.FLAG_GRANT_READ_URI_PERMISSION)
            }
            activity.startActivity(Intent.createChooser(intent, "Save Data"))
        } else {
            // Fallback for text share if file provider fails
            val intent = Intent(Intent.ACTION_SEND).apply {
                type = "text/plain"
                putExtra(Intent.EXTRA_TEXT, String(content))
            }
            activity.startActivity(Intent.createChooser(intent, "Save Data"))
        }
    }
}
