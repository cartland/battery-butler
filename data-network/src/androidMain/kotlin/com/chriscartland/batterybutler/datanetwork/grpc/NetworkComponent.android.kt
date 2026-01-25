
package com.chriscartland.batterybutler.datanetwork.grpc

import android.content.Context
import com.squareup.wire.GrpcClient
import okhttp3.OkHttpClient
import okhttp3.Protocol

actual class NetworkComponent(
    private val context: Context,
) {
    actual fun createGrpcClient(url: String): GrpcClient =
        GrpcClient
            .Builder()
            .client(
                OkHttpClient
                    .Builder()
                    .protocols(listOf(Protocol.H2_PRIOR_KNOWLEDGE))
                    .build(),
            ).baseUrl(url)
            .build()
}
