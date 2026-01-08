package com.chriscartland.batterybutler

class BatteryService : BatteryServiceGrpcKt.BatteryServiceCoroutineImplBase() {
    override suspend fun getServerStatus(
        request: BatteryServiceOuterClass.ServerStatusRequest,
    ): BatteryServiceOuterClass.ServerStatusResponse =
        BatteryServiceOuterClass.ServerStatusResponse
            .newBuilder()
            .setIsAlive(true)
            .setVersion("1.0.0")
            .setMessage("Battery Butler Server is running")
            .build()
}
