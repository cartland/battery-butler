package com.chriscartland.batterybutler.e2e

import com.chriscartland.batterybutler.proto.SyncUpdate
import kotlinx.coroutines.coroutineScope
import kotlinx.coroutines.runBlocking
import kotlinx.coroutines.withTimeout
import org.junit.Test
import kotlin.test.assertTrue
import kotlin.time.Duration.Companion.seconds

/**
 * Test subscribing to the server's data stream.
 *
 * Verifies that the Subscribe RPC returns an initial full snapshot.
 */
class SyncSubscribeE2eTest : E2eTestBase() {
    @Test
    fun `Subscribe receives initial full snapshot`() =
        runBlocking {
            withTimeout(15.seconds) {
                coroutineScope {
                    val (requestChannel, responseChannel) = syncClient.Subscribe().executeIn(this)

                    // Send the initial (empty) request to trigger the stream
                    requestChannel.send(Unit)
                    // Half-close so the server invokes the handler
                    requestChannel.close()

                    val firstUpdate: SyncUpdate = responseChannel.receive()
                    assertTrue(firstUpdate.is_full_snapshot, "First update should be a full snapshot")
                }
            }
        }
}
