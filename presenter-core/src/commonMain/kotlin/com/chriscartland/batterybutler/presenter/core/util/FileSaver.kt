package com.chriscartland.batterybutler.presenter.core.util

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
