package com.chriscartland.batterybutler.e2e

import kotlinx.coroutines.runBlocking
import kotlinx.coroutines.withTimeout
import org.junit.Test
import kotlin.test.assertTrue
import kotlin.time.Duration.Companion.seconds

/**
 * Test subscribing to the server's data stream.
 * The server sends an initial full snapshot immediately after subscribe.
 */
class SyncSubscribeE2eTest : E2eTestBase() {
    @Test
    fun `Subscribe receives initial full snapshot`() =
        runBlocking {
            withTimeout(30.seconds) {
                val (requestChannel, responseChannel) = syncClient.Subscribe().executeIn(this)

                // CRITICAL: Must send initial request to trigger server streaming
                requestChannel.send(Unit)

                val firstUpdate = responseChannel.receive()

                assertTrue(firstUpdate.is_full_snapshot, "First update should be a full snapshot")
                assertTrue(
                    firstUpdate.device_types.isNotEmpty(),
                    "Snapshot should contain device types (server has demo data)",
                )
                assertTrue(
                    firstUpdate.devices.isNotEmpty(),
                    "Snapshot should contain devices (server has demo data)",
                )

                requestChannel.close()
            }
        }
}
