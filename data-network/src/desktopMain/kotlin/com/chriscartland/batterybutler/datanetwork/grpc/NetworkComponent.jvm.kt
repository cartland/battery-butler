package com.chriscartland.batterybutler.datanetwork.grpc

import com.squareup.wire.GrpcClient
import okhttp3.OkHttpClient

actual class NetworkComponent {
    actual fun createGrpcClient(): GrpcClient =
        GrpcClient
            .Builder()
            .client(OkHttpClient.Builder().build())
            .baseUrl("http://localhost:50051")
            .build()
}
