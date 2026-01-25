package com.chriscartland.batterybutler.datanetwork.grpc

import com.squareup.wire.GrpcClient
import okhttp3.OkHttpClient

actual class NetworkComponent {
    actual fun createGrpcClient(url: String): GrpcClient =
        GrpcClient
            .Builder()
            .client(
                OkHttpClient
                    .Builder()
                    .protocols(listOf(okhttp3.Protocol.H2_PRIOR_KNOWLEDGE))
                    .build(),
            ).baseUrl(url)
            .build()
}
