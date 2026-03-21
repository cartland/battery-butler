package com.chriscartland.batterybutler.datalocal.room

import com.chriscartland.batterybutler.domain.model.NetworkMode

enum class DatabaseOption(
    val fileName: String,
) {
    Offline("battery-butler-offline.db"),
    Mock("battery-butler-mock.db"),
    LocalServer("battery-butler-local-server.db"),
    ProductionServer("battery-butler-production-server.db"),
    DevServer("battery-butler-dev-server.db"),
    ;

    companion object {
        /** File names from android/27 and earlier releases. */
        val legacyFileNames: Map<DatabaseOption, String> = mapOf(
            Offline to "battery-butler.db",
            Mock to "battery-butler-dev.db",
        )

        fun fromNetworkMode(mode: NetworkMode): DatabaseOption =
            when (mode) {
                NetworkMode.None -> Offline
                NetworkMode.Mock -> Mock
                is NetworkMode.GrpcLocal -> LocalServer
                is NetworkMode.GrpcAws -> ProductionServer
                is NetworkMode.GrpcDev -> DevServer
            }
    }
}
