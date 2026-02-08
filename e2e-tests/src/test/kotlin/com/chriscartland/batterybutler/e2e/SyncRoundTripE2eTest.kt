package com.chriscartland.batterybutler.e2e

import org.junit.Ignore
import org.junit.Test

/**
 * Round-trip test: push test data, then subscribe and verify it appears in the snapshot.
 *
 * Currently blocked by the same issues as SyncSubscribeE2eTest:
 * 1. Wire's GrpcStreamingCall doesn't send HTTP/2 half-close for server-streaming RPCs.
 * 2. PostgresDeviceRepository.getUpdates() is a stub (empty flow).
 *
 * Enable when both issues are resolved.
 */
class SyncRoundTripE2eTest : E2eTestBase() {
    @Ignore("Blocked: Wire GrpcStreamingCall half-close + server getUpdates() stub")
    @Test
    fun `pushed data appears in subscribe snapshot`() {
        // TODO: Enable when Wire half-close is fixed and server Subscribe is implemented.
        // Expected flow:
        //   push test data via PushUpdate
        //   subscribe and receive snapshot
        //   assert snapshot contains pushed data
    }
}
