package com.chriscartland.batterybutler.datalocal.room

enum class DatabaseOption(
    val fileName: String,
) {
    Offline("battery-butler-offline.db"),
    Mock("battery-butler-mock.db"),
    LocalServer("battery-butler-local-server.db"),
    ProductionServer("battery-butler-production-server.db"),
    DevServer("battery-butler-dev-server.db"),
}
