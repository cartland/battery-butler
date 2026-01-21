package com.chriscartland.batterybutler.networking

import android.content.Context
import com.squareup.wire.GrpcClient
import okhttp3.OkHttpClient

actual class NetworkComponent(
    private val context: Context,
) {
    actual fun createGrpcClient(): GrpcClient =
        GrpcClient
            .Builder()
            .client(
                OkHttpClient
                    .Builder()
                    .protocols(listOf(okhttp3.Protocol.H2_PRIOR_KNOWLEDGE))
                    .build(),
            ).baseUrl(LOCAL_GRPC_ADDRESS)
            .build()

    companion object {
        private const val LOCAL_GRPC_ADDRESS = "http://10.0.2.2:50051"
    }
}
