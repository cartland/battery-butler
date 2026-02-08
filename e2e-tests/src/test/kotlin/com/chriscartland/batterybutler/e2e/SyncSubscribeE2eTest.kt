package com.chriscartland.batterybutler.e2e

import org.junit.Ignore
import org.junit.Test

/**
 * Test subscribing to the server's data stream.
 *
 * Currently blocked by two issues:
 * 1. Wire's GrpcStreamingCall doesn't send HTTP/2 half-close after the request message,
 *    so grpc-java's server never invokes the subscribe() handler.
 * 2. PostgresDeviceRepository.getUpdates() is a stub that returns an empty flow.
 *
 * Enable these tests when both issues are resolved.
 */
class SyncSubscribeE2eTest : E2eTestBase() {
    @Ignore("Blocked: Wire GrpcStreamingCall half-close + server getUpdates() stub")
    @Test
    fun `Subscribe receives initial full snapshot`() {
        // TODO: Enable when Wire half-close is fixed and server Subscribe is implemented.
        // Expected flow:
        //   val (requestChannel, responseChannel) = syncClient.Subscribe().executeIn(this)
        //   requestChannel.send(Unit)
        //   val firstUpdate = responseChannel.receive()
        //   assertTrue(firstUpdate.is_full_snapshot)
    }
}
