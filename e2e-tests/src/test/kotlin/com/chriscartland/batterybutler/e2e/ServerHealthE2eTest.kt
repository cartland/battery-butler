package com.chriscartland.batterybutler.e2e

import com.chriscartland.batterybutler.proto.ServerStatusRequest
import kotlinx.coroutines.runBlocking
import org.junit.Test
import kotlin.test.assertEquals
import kotlin.test.assertTrue

/**
 * Smoke test: verify the server is alive and responding to gRPC.
 * If this fails, all other E2E tests will too.
 */
class ServerHealthE2eTest : E2eTestBase() {
    @Test
    fun `GetServerStatus returns alive`() =
        runBlocking {
            val response = batteryClient.GetServerStatus().execute(ServerStatusRequest())

            assertTrue(response.is_alive, "Server should report is_alive=true")
            assertEquals("1.0.0", response.version, "Server version should be 1.0.0")
            assertTrue(response.message.isNotEmpty(), "Server message should be non-empty")
        }
}
