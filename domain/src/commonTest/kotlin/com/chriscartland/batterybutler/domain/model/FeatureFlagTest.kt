package com.chriscartland.batterybutler.domain.model

import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertTrue

class FeatureFlagTest {
    @Test
    fun `FeatureFlag enum has expected values`() {
        val flags = FeatureFlag.entries
        assertEquals(3, flags.size)
        assertTrue(flags.contains(FeatureFlag.AI_BATCH_IMPORT))
        assertTrue(flags.contains(FeatureFlag.REMOTE_SYNC))
        assertTrue(flags.contains(FeatureFlag.LEGACY_DATA_MODES))
    }

    @Test
    fun `FeatureFlag names are correct`() {
        assertEquals("AI_BATCH_IMPORT", FeatureFlag.AI_BATCH_IMPORT.name)
        assertEquals("REMOTE_SYNC", FeatureFlag.REMOTE_SYNC.name)
        assertEquals("LEGACY_DATA_MODES", FeatureFlag.LEGACY_DATA_MODES.name)
    }
}
