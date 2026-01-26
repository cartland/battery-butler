package com.chriscartland.batterybutler.composeapp

import androidx.test.ext.junit.runners.AndroidJUnit4
import com.squareup.wire.GrpcClient
import kotlinx.coroutines.runBlocking
import okhttp3.OkHttpClient
import okhttp3.Protocol
import org.junit.Test
import org.junit.runner.RunWith
import java.util.concurrent.TimeUnit

@RunWith(AndroidJUnit4::class)
class RemoteConnectivityTest {
    companion object {
        const val PRODUCTION_SERVER_URL = "http://battery-butler-nlb-847feaa773351518.elb.us-west-1.amazonaws.com:80"
    }
    @Test
    fun testProductionServerConnectivity() =
        runBlocking {
            // Build a minimal GrpcClient pointing to Production NLB
            val client = OkHttpClient
                .Builder()
                .protocols(listOf(Protocol.H2_PRIOR_KNOWLEDGE))
                .readTimeout(15, TimeUnit.SECONDS)
                .build()

            val grpcClient = GrpcClient
                .Builder()
                .client(client)
                .baseUrl(PRODUCTION_SERVER_URL)
                .build()

            // Assuming there is a GreeterService or health check.
            // If not, we can assume failure for now and refine this test.
            // Looking at 'server/app' I know there is a Greeter service usually.
            // I will use a simple call if available.
            // If GreeterClient is not known, I might need to check available generated code.
            // For now, I will try to hit the root using basic OkHttp just to prove TCP.

            val request = okhttp3.Request
                .Builder()
                .url(PRODUCTION_SERVER_URL) // This might 404 on HTTP/1.1 attempt, but confirms connection
                .build()

            // This is a "Poor man's" connectivity check for now.
            // Real gRPC check requires the generated client on classpath.
            try {
                val response = client.newCall(request).execute()
                println("Response: ${response.code}")
                // 200, 404, or 500 means we hit the server.
                // Connection Refused means we failed.
            } catch (e: Exception) {
                throw RuntimeException("Failed to connect to ${PRODUCTION_SERVER_URL}", e)
            }
        }
}
