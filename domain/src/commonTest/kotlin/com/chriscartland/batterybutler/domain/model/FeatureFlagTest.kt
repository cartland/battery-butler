package com.chriscartland.batterybutler.domain.model

import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertTrue

class FeatureFlagTest {
    @Test
    fun `FeatureFlag enum has expected values`() {
        val flags = FeatureFlag.entries
        assertEquals(2, flags.size)
        assertTrue(flags.contains(FeatureFlag.AI_BATCH_IMPORT))
        assertTrue(flags.contains(FeatureFlag.REMOTE_SYNC))
    }

    @Test
    fun `FeatureFlag names are correct`() {
        assertEquals("AI_BATCH_IMPORT", FeatureFlag.AI_BATCH_IMPORT.name)
        assertEquals("REMOTE_SYNC", FeatureFlag.REMOTE_SYNC.name)
    }
}
