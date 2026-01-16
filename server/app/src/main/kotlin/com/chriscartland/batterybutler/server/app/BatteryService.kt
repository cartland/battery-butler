package com.chriscartland.batterybutler.server.app

import com.chriscartland.batterybutler.proto.BatteryServiceGrpcKt
import com.chriscartland.batterybutler.proto.ServerStatusRequest
import com.chriscartland.batterybutler.proto.ServerStatusResponse

class BatteryService : BatteryServiceGrpcKt.BatteryServiceCoroutineImplBase() {
    override suspend fun getServerStatus(request: ServerStatusRequest): ServerStatusResponse =
        ServerStatusResponse
            .newBuilder()
            .setIsAlive(true)
            .setVersion("1.0.0")
            .setMessage("Battery Butler Server is running")
            .build()
}
