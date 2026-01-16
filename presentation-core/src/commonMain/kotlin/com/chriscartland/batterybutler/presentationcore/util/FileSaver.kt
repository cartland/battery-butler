package com.chriscartland.batterybutler.presentationcore.util

import androidx.compose.runtime.compositionLocalOf

interface FileSaver {
    fun saveFile(
        fileName: String,
        content: ByteArray,
    )
}

val LocalFileSaver = compositionLocalOf<FileSaver> {
    error("No FileSaver provided")
}
