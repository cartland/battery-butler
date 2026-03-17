package com.chriscartland.batterybutler.datalocal.room

enum class DatabaseOption(
    val fileName: String,
) {
    Production("battery-butler.db"),
    Development("battery-butler-dev.db"),
}
