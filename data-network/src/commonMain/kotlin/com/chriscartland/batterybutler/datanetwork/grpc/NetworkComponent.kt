package com.chriscartland.batterybutler.datanetwork.grpc

import com.squareup.wire.GrpcClient

expect class NetworkComponent() {
    fun createGrpcClient(
        url: String,
        dispatcherProvider: com.chriscartland.batterybutler.domain.model.DispatcherProvider,
    ): com.squareup.wire.GrpcClient
}
