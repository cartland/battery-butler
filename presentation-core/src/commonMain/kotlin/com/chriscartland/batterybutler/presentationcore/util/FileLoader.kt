package com.chriscartland.batterybutler.presentationcore.util

import androidx.compose.runtime.compositionLocalOf

interface FileLoader {
    fun loadFile(onResult: (ByteArray?) -> Unit)
}

val LocalFileLoader = compositionLocalOf<FileLoader> {
    error("No FileLoader provided")
}
