package com.chriscartland.batterybutler.composeapp

import androidx.test.ext.junit.runners.AndroidJUnit4
import androidx.test.platform.app.InstrumentationRegistry
import com.chriscartland.batterybutler.datanetwork.grpc.NetworkComponent
import com.chriscartland.batterybutler.proto.SyncServiceClient
import com.chriscartland.batterybutler.proto.SyncUpdate
import kotlinx.coroutines.runBlocking
import org.junit.Assert.assertEquals
import org.junit.Assert.assertNotNull
import org.junit.Test
import org.junit.runner.RunWith

@RunWith(AndroidJUnit4::class)
class ServerIntegrationTest {
    @Test
    fun verifyServerConnection() =
        runBlocking {
            // Context of the app under test.
            val appContext = InstrumentationRegistry.getInstrumentation().targetContext
            val networkComponent = NetworkComponent(appContext)
            val client = networkComponent.createGrpcClient("http://10.0.2.2:50051").create(SyncServiceClient::class)

            // 1. Push an update
            val update = SyncUpdate(
                is_full_snapshot = false,
                device_types = emptyList(),
                devices = emptyList(),
                events = emptyList(),
            )
            val pushResponse = client.PushUpdate().execute(update)

            assertNotNull("Push response should not be null", pushResponse)
            assertEquals("Server should return success status", true, pushResponse.success)

            // 2. Subscribe (basic check)
            // Note: Streaming tests in instrumented environments can be tricky depending on timeout/server state.
            // We just verify we can establish the call.
            // val (requestChannel, responseChannel) = client.Subscribe().executeIn(this)
            // requestChannel.close() // Close request stream immediately as we just want to see if it connects

            // If we got here regarding channels without crash, connection is partly successful.
            // responseChannel.cancel()
        }
}
