package com.chriscartland.batterybutler.domain

import android.util.Log

actual object AppLogger {
    actual fun d(
        tag: String,
        message: String,
    ) {
        Log.d(tag, message)
    }
}
