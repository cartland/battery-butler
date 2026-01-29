package com.chriscartland.batterybutler.domain.model

import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertIs
import kotlin.test.assertNotEquals
import kotlin.test.assertTrue

class SyncStatusTest {
    @Test
    fun `Idle is singleton instance`() {
        val idle1 = SyncStatus.Idle
        val idle2 = SyncStatus.Idle

        assertEquals(idle1, idle2)
    }

    @Test
    fun `Syncing is singleton instance`() {
        val syncing1 = SyncStatus.Syncing
        val syncing2 = SyncStatus.Syncing

        assertEquals(syncing1, syncing2)
    }

    @Test
    fun `Success is singleton instance`() {
        val success1 = SyncStatus.Success
        val success2 = SyncStatus.Success

        assertEquals(success1, success2)
    }

    @Test
    fun `Failed stores message`() {
        val failed = SyncStatus.Failed(message = "Network error")

        assertEquals("Network error", failed.message)
    }

    @Test
    fun `Failed instances with same message are equal`() {
        val failed1 = SyncStatus.Failed(message = "Connection timeout")
        val failed2 = SyncStatus.Failed(message = "Connection timeout")

        assertEquals(failed1, failed2)
        assertEquals(failed1.hashCode(), failed2.hashCode())
    }

    @Test
    fun `Failed instances with different messages are not equal`() {
        val failed1 = SyncStatus.Failed(message = "Error 1")
        val failed2 = SyncStatus.Failed(message = "Error 2")

        assertNotEquals(failed1, failed2)
    }

    @Test
    fun `sealed interface variants are distinguishable`() {
        val idle: SyncStatus = SyncStatus.Idle
        val syncing: SyncStatus = SyncStatus.Syncing
        val success: SyncStatus = SyncStatus.Success
        val failed: SyncStatus = SyncStatus.Failed("error")

        assertIs<SyncStatus.Idle>(idle)
        assertIs<SyncStatus.Syncing>(syncing)
        assertIs<SyncStatus.Success>(success)
        assertIs<SyncStatus.Failed>(failed)
    }

    @Test
    fun `when expression covers all variants`() {
        val statuses = listOf(
            SyncStatus.Idle,
            SyncStatus.Syncing,
            SyncStatus.Success,
            SyncStatus.Failed("error"),
        )

        for (status in statuses) {
            val description = when (status) {
                is SyncStatus.Idle -> "idle"
                is SyncStatus.Syncing -> "syncing"
                is SyncStatus.Success -> "success"
                is SyncStatus.Failed -> "failed"
            }
            // If we get here without error, the when is exhaustive
            assertTrue(description.isNotEmpty())
        }
    }

    @Test
    fun `singleton instances are not equal to each other`() {
        assertNotEquals<SyncStatus>(SyncStatus.Idle, SyncStatus.Syncing)
        assertNotEquals<SyncStatus>(SyncStatus.Syncing, SyncStatus.Success)
        assertNotEquals<SyncStatus>(SyncStatus.Idle, SyncStatus.Success)
    }
}
