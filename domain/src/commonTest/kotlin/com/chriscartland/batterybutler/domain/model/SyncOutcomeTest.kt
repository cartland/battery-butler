package com.chriscartland.batterybutler.domain.model

import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertNotEquals
import kotlin.test.assertTrue

class SyncOutcomeTest {
    @Test
    fun `when expression covers all variants`() {
        val outcomes = listOf(
            SyncOutcome.Success,
            SyncOutcome.Skipped,
            SyncOutcome.AuthRequired(SyncAuthReason.NO_SESSION),
            SyncOutcome.Failed(DataError.Unknown("error")),
        )

        for (outcome in outcomes) {
            val description = when (outcome) {
                SyncOutcome.Success -> "success"
                SyncOutcome.Skipped -> "skipped"
                is SyncOutcome.AuthRequired -> "auth required"
                is SyncOutcome.Failed -> "failed"
            }
            assertTrue(description.isNotEmpty())
        }
    }

    @Test
    fun `AuthRequired carries its reason with value equality`() {
        assertEquals(
            SyncOutcome.AuthRequired(SyncAuthReason.TOKEN_EXPIRED),
            SyncOutcome.AuthRequired(SyncAuthReason.TOKEN_EXPIRED),
        )
        assertNotEquals(
            SyncOutcome.AuthRequired(SyncAuthReason.TOKEN_EXPIRED),
            SyncOutcome.AuthRequired(SyncAuthReason.TOKEN_INVALID),
        )
    }

    @Test
    fun `Failed carries the typed error`() {
        val failed = SyncOutcome.Failed(DataError.Network.ServerError(cause = "HTTP 500"))

        assertEquals("HTTP 500", failed.error.cause)
        assertNotEquals<SyncOutcome>(failed, SyncOutcome.Success)
    }
}
