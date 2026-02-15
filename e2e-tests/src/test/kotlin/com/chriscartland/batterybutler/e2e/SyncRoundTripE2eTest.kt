package com.chriscartland.batterybutler.e2e

import com.chriscartland.batterybutler.proto.SyncUpdate
import kotlinx.coroutines.coroutineScope
import kotlinx.coroutines.runBlocking
import kotlinx.coroutines.withTimeout
import org.junit.Test
import kotlin.test.assertTrue
import kotlin.time.Duration.Companion.seconds

/**
 * Round-trip test: push test data, then subscribe and verify it appears in the snapshot.
 */
class SyncRoundTripE2eTest : E2eTestBase() {
    @Test
    fun `pushed data appears in subscribe snapshot`() =
        runBlocking {
            // 1. Push test data
            val deviceType = testDeviceType()
            val device = testDevice(typeId = deviceType.id)

            val pushUpdate = SyncUpdate(
                is_full_snapshot = false,
                device_types = listOf(deviceType),
                devices = listOf(device),
            )
            val pushResponse = syncClient.PushUpdate().execute(pushUpdate)
            assertTrue(pushResponse.success, "PushUpdate should succeed")

            // 2. Subscribe and verify the pushed data appears in the snapshot
            withTimeout(15.seconds) {
                coroutineScope {
                    val (requestChannel, responseChannel) = syncClient.Subscribe().executeIn(this)
                    requestChannel.send(Unit)
                    requestChannel.close()

                    val snapshot: SyncUpdate = responseChannel.receive()
                    assertTrue(snapshot.is_full_snapshot, "First update should be a full snapshot")

                    val foundDevice = snapshot.devices.any { it.id == device.id }
                    assertTrue(foundDevice, "Snapshot should contain the pushed device (id=${device.id})")

                    val foundType = snapshot.device_types.any { it.id == deviceType.id }
                    assertTrue(foundType, "Snapshot should contain the pushed device type (id=${deviceType.id})")
                }
            }
        }
}
