package com.chriscartland.batterybutler.domain

actual object AppLogger {
    actual fun d(
        tag: String,
        message: String,
    ) {
        println("$tag: $message")
    }
}
