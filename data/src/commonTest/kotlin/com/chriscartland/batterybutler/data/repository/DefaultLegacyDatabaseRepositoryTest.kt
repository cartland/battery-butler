package com.chriscartland.batterybutler.data.repository

import com.chriscartland.batterybutler.domain.model.NetworkMode
import com.chriscartland.batterybutler.testcommon.FakeLegacyDatabaseRepository
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertNotNull
import kotlin.test.assertNull
import kotlin.test.assertTrue

/**
 * Tests for [DefaultLegacyDatabaseRepository] behavior via the domain interface.
 *
 * Since DefaultLegacyDatabaseRepository depends on expect-class DatabaseFactory,
 * we test the domain contract through [FakeLegacyDatabaseRepository] which
 * mirrors the same interface, and verify the mapping logic in DatabaseFactoryTest.
 */
class DefaultLegacyDatabaseRepositoryTest {
    @Test
    fun `getLegacyDatabaseInfo returns null for mode without legacy file`() {
        val repo = FakeLegacyDatabaseRepository()

        val result = repo.getLegacyDatabaseInfo(NetworkMode.GrpcLocal("http://localhost:50051"))

        assertNull(result)
    }

    @Test
    fun `getLegacyDatabaseInfo returns info when legacy file exists`() {
        val repo = FakeLegacyDatabaseRepository()
        repo.legacyInfoByMode[NetworkMode.None] = com.chriscartland.batterybutler.domain.model.LegacyDatabaseInfo(
            legacyFileName = "battery-butler.db",
            exists = true,
        )

        val result = repo.getLegacyDatabaseInfo(NetworkMode.None)

        assertNotNull(result)
        assertEquals("battery-butler.db", result.legacyFileName)
        assertTrue(result.exists)
    }

    @Test
    fun `getCurrentDatabaseFileName returns file name for mode`() {
        val repo = FakeLegacyDatabaseRepository()
        repo.fileNameByMode[NetworkMode.None] = "battery-butler-offline.db"

        val result = repo.getCurrentDatabaseFileName(NetworkMode.None)

        assertEquals("battery-butler-offline.db", result)
    }
}
