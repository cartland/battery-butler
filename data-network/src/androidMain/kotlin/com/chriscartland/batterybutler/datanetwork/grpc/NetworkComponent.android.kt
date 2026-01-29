
package com.chriscartland.batterybutler.datanetwork.grpc

import android.content.Context
import com.squareup.wire.GrpcClient
import okhttp3.OkHttpClient
import okhttp3.Protocol
import java.util.concurrent.TimeUnit

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
                    .connectTimeout(CONNECT_TIMEOUT_SECONDS, TimeUnit.SECONDS)
                    .readTimeout(READ_TIMEOUT_SECONDS, TimeUnit.SECONDS)
                    .writeTimeout(WRITE_TIMEOUT_SECONDS, TimeUnit.SECONDS)
                    .callTimeout(CALL_TIMEOUT_SECONDS, TimeUnit.SECONDS)
                    .build(),
            ).baseUrl(url)
            .build()

    companion object {
        private const val CONNECT_TIMEOUT_SECONDS = 10L
        private const val READ_TIMEOUT_SECONDS = 30L
        private const val WRITE_TIMEOUT_SECONDS = 30L
        private const val CALL_TIMEOUT_SECONDS = 60L
    }
}
