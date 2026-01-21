package com.chriscartland.batterybutler.domain

expect object AppLogger {
    fun d(
        tag: String,
        message: String,
    )
}
