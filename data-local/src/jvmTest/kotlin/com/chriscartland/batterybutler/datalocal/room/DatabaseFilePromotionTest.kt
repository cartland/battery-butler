package com.chriscartland.batterybutler.datalocal.room

import com.chriscartland.batterybutler.datalocal.room.entity.toEntity
import com.chriscartland.batterybutler.domain.model.DeviceType
import com.chriscartland.batterybutler.domain.model.NetworkMode
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.test.runTest
import java.io.File
import java.util.UUID
import kotlin.test.AfterTest
import kotlin.test.BeforeTest
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertFalse
import kotlin.test.assertTrue

/**
 * Verifies [promoteBareFileIfNeeded] (wired into each platform's `createNewDatabase`): an existing
 * install's bare per-category file is inherited by the first url-suffixed configuration that opens
 * it, but a *different* url under the same category never sees that data — this is the actual
 * mixing guarantee the url-aware [DatabaseOption] key exists to provide.
 */
class DatabaseFilePromotionTest {
    private val tmpDir = File(System.getProperty("java.io.tmpdir"))
    private val prodBareFileName = DatabaseOption.baseFileNames.getValue(DatabaseCategory.ProductionServer)
    private val urlA = "https://prod-a.example.com"
    private val urlB = "https://prod-b.example.com"

    @BeforeTest
    @AfterTest
    fun cleanDbFiles() {
        DatabaseOption.baseFileNames.values.forEach { File(tmpDir, it).delete() }
        listOf(urlA, urlB).forEach {
            File(tmpDir, DatabaseOption.fromNetworkMode(NetworkMode.GrpcAws(it)).fileName).delete()
        }
    }

    private fun marker() = DeviceType(id = UUID.randomUUID().toString(), name = "Marker", batteryType = "AA", defaultIcon = null)

    @Test
    fun `configured url inherits an existing bare file's data, and the bare file is consumed`() =
        runTest {
            val factory = DatabaseFactory()
            val bareOption = DatabaseOption(DatabaseCategory.ProductionServer, prodBareFileName)
            val type = marker()

            // Seed a bare (pre-upgrade-style) file directly, bypassing fromNetworkMode.
            val seedDb = factory.createDatabase(bareOption)
            seedDb.deviceDao().insertDeviceType(type.toEntity())
            seedDb.close()
            factory.evict(bareOption)
            assertTrue(File(tmpDir, prodBareFileName).exists(), "bare file should exist after seeding")

            val promoted = factory.createDatabase(DatabaseOption.fromNetworkMode(NetworkMode.GrpcAws(urlA)))
            val types = promoted.deviceDao().getAllDeviceTypes().first()

            assertEquals(1, types.size)
            assertEquals(type.id, types.single().id)
            assertFalse(File(tmpDir, prodBareFileName).exists(), "bare file should be renamed away, not copied")
        }

    @Test
    fun `a different url under the same category never sees the other url's data`() =
        runTest {
            val factory = DatabaseFactory()
            val bareOption = DatabaseOption(DatabaseCategory.ProductionServer, prodBareFileName)
            val type = marker()

            val seedDb = factory.createDatabase(bareOption)
            seedDb.deviceDao().insertDeviceType(type.toEntity())
            seedDb.close()
            factory.evict(bareOption)

            // urlA promotes/inherits the bare file's data.
            val dbA = factory.createDatabase(DatabaseOption.fromNetworkMode(NetworkMode.GrpcAws(urlA)))
            assertEquals(
                1,
                dbA
                    .deviceDao()
                    .getAllDeviceTypes()
                    .first()
                    .size,
            )

            // urlB is a different backend under the same category: must start empty, no bleed-through.
            val dbB = factory.createDatabase(DatabaseOption.fromNetworkMode(NetworkMode.GrpcAws(urlB)))
            assertTrue(
                dbB
                    .deviceDao()
                    .getAllDeviceTypes()
                    .first()
                    .isEmpty(),
                "a different url must never inherit another url's data",
            )
        }

    @Test
    fun `re-opening the same url after evict reuses the already-promoted file`() =
        runTest {
            val factory = DatabaseFactory()
            val bareOption = DatabaseOption(DatabaseCategory.ProductionServer, prodBareFileName)
            val type = marker()
            val option = DatabaseOption.fromNetworkMode(NetworkMode.GrpcAws(urlA))

            val seedDb = factory.createDatabase(bareOption)
            seedDb.deviceDao().insertDeviceType(type.toEntity())
            seedDb.close()
            factory.evict(bareOption)

            val firstOpen = factory.createDatabase(option)
            assertEquals(
                1,
                firstOpen
                    .deviceDao()
                    .getAllDeviceTypes()
                    .first()
                    .size,
            )
            firstOpen.close()
            factory.evict(option)

            // Second open: bare file is already gone, so this must reopen the promoted file directly.
            val secondOpen = factory.createDatabase(option)
            val types = secondOpen.deviceDao().getAllDeviceTypes().first()
            assertEquals(1, types.size, "re-opening the same url must not lose or duplicate data")
            assertEquals(type.id, types.single().id)
        }
}
