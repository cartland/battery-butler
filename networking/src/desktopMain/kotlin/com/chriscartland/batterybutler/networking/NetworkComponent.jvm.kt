package com.chriscartland.batterybutler.networking

import com.squareup.wire.GrpcClient
import okhttp3.OkHttpClient

actual class NetworkComponent {
    actual val grpcClient: GrpcClient by lazy {
        GrpcClient.Builder()
            .client(OkHttpClient.Builder().build())
            .baseUrl("http://localhost:50051")
            .build()
    }
}
