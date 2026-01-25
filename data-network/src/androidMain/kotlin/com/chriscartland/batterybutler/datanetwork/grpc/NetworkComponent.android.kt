
package com.chriscartland.batterybutler.datanetwork.grpc

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
            ).baseUrl(SharedServerConfig.PRODUCTION_SERVER_URL)
            .build()
}
