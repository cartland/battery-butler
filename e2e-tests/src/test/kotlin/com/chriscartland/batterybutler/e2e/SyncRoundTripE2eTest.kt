package com.chriscartland.batterybutler.e2e

import com.chriscartland.batterybutler.proto.SyncUpdate
import kotlinx.coroutines.runBlocking
import kotlinx.coroutines.withTimeout
import org.junit.Test
import kotlin.test.assertNotNull
import kotlin.test.assertTrue
import kotlin.time.Duration.Companion.seconds

/**
 * Round-trip test: push test data, then subscribe and verify it appears in the snapshot.
 */
class SyncRoundTripE2eTest : E2eTestBase() {
    @Test
    fun `pushed data appears in subscribe snapshot`() =
        runBlocking {
            val deviceType = testDeviceType()
            val device = testDevice(typeId = deviceType.id)

            // Push test data
            val pushUpdate = SyncUpdate(
                is_full_snapshot = false,
                device_types = listOf(deviceType),
                devices = listOf(device),
            )
            val pushResponse = syncClient.PushUpdate().execute(pushUpdate)
            assertTrue(pushResponse.success, "Push should succeed before subscribe")

            // Subscribe and verify data appears in snapshot
            withTimeout(30.seconds) {
                val (requestChannel, responseChannel) = syncClient.Subscribe().executeIn(this)
                requestChannel.send(Unit)

                val snapshot = responseChannel.receive()
                assertTrue(snapshot.is_full_snapshot, "First update should be full snapshot")

                val foundDeviceType = snapshot.device_types.find { it.id == deviceType.id }
                assertNotNull(foundDeviceType, "Snapshot should contain pushed device type ${deviceType.id}")

                val foundDevice = snapshot.devices.find { it.id == device.id }
                assertNotNull(foundDevice, "Snapshot should contain pushed device ${device.id}")

                requestChannel.close()
            }
        }
}
