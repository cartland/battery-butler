package com.chriscartland.batterybutler

interface Platform {
    val name: String
}

expect fun getPlatform(): Platform
