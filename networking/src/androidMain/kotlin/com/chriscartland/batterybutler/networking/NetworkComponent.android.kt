package com.chriscartland.batterybutler.networking

import android.content.Context
import com.squareup.wire.GrpcClient
import okhttp3.OkHttpClient

actual class NetworkComponent(
    private val context: Context
) {
    actual val grpcClient: GrpcClient by lazy {
        GrpcClient.Builder()
            .client(
                OkHttpClient.Builder()
                    .protocols(listOf(okhttp3.Protocol.H2_PRIOR_KNOWLEDGE))
                    .build()
            )
            .baseUrl("http://10.0.2.2:50051")
            .build()
    }
}
