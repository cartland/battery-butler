package com.chriscartland.batterybutler.usecase

import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertFalse
import kotlin.test.assertTrue

class AiReplyGuardTest {
    @Test
    fun `strips code fences and tool calls and keeps the prose`() {
        assertEquals("Sure.", AiReplyGuard.clean("Sure.\n```tool_code\nx()\n```\n{\"tool\": \"a\", \"args\": {}}\nrecordBatteryReplacement(deviceName=\"Y\")"))
    }

    @Test
    fun `recognises claims of a change`() {
        listOf("I've added it.", "I added the alarm", "The device has been deleted", "It was recorded", "I have updated the name").forEach {
            assertTrue(AiReplyGuard.claimsAChange(it), it)
        }
    }

    @Test
    fun `leaves plain answers alone`() {
        listOf("Your remote takes AAA.", "Two devices are due.", "Should I add it?").forEach {
            assertFalse(AiReplyGuard.claimsAChange(it), it)
        }
    }
}
