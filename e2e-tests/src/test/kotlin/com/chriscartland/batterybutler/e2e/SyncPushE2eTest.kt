package com.chriscartland.batterybutler.e2e

import com.chriscartland.batterybutler.proto.SyncUpdate
import kotlinx.coroutines.runBlocking
import org.junit.Test
import kotlin.test.assertTrue

/**
 * Test pushing data to the server via PushUpdate RPC.
 */
class SyncPushE2eTest : E2eTestBase() {
    @Test
    fun `PushUpdate with test data returns success`() =
        runBlocking {
            val deviceType = testDeviceType()
            val device = testDevice(typeId = deviceType.id)

            val update = SyncUpdate(
                is_full_snapshot = false,
                device_types = listOf(deviceType),
                devices = listOf(device),
            )

            val response = syncClient.PushUpdate().execute(update)

            assertTrue(response.success, "PushUpdate should return success=true, got message: ${response.message}")
        }
}
