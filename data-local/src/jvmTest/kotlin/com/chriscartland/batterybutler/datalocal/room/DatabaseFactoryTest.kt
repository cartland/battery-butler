package com.chriscartland.batterybutler.datalocal.room

import com.chriscartland.batterybutler.domain.model.DataMode
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertNotSame
import kotlin.test.assertNull
import kotlin.test.assertSame

/** One representative [DataMode] per [DatabaseCategory], mirroring the app's standard Settings options. */
private val representativeModes = listOf(
    DataMode.None,
    DataMode.Mock,
    DataMode.GrpcLocal("http://localhost:50051"),
    DataMode.GrpcAws("http://prod:443"),
    DataMode.GrpcDev("http://dev:443"),
    DataMode.LabsStaging("https://staging.example.com"),
    DataMode.LabsProd("https://prod.example.com"),
)

class DatabaseFactoryTest {
    /**
     * LOCKED DATABASE FILE NAMES — DO NOT CHANGE WITHOUT MIGRATION.
     *
     * These file names are persisted on user devices. Changing a base file name without adding
     * migration logic (renaming the old file to the new name BEFORE Room opens it — see
     * [promoteBareFileIfNeeded]) will cause users to lose all local data. See also
     * `DatabaseOptionTest` for the url-aware key-derivation tests.
     *
     * If you need to rename a database file:
     * 1. Add migration code in DatabaseFactory (each platform) that renames
     *    the old file to the new name before createDatabase() is called.
     * 2. Update this test to reflect the new file name.
     * 3. Add the old file name to the migration logic so it is handled.
     */
    @Test
    fun `database file names must not change without migration`() {
        assertEquals("battery-butler-offline.db", DatabaseOption.baseFileNames.getValue(DatabaseCategory.Offline))
        assertEquals("battery-butler-mock.db", DatabaseOption.baseFileNames.getValue(DatabaseCategory.Mock))
        assertEquals("battery-butler-local-server.db", DatabaseOption.baseFileNames.getValue(DatabaseCategory.LocalServer))
        assertEquals("battery-butler-production-server.db", DatabaseOption.baseFileNames.getValue(DatabaseCategory.ProductionServer))
        assertEquals("battery-butler-dev-server.db", DatabaseOption.baseFileNames.getValue(DatabaseCategory.DevServer))
    }

    @Test
    fun `the standard per-category configurations have unique file names`() {
        val fileNames = representativeModes.map { DatabaseOption.fromDataMode(it).fileName }
        assertEquals(fileNames.size, fileNames.toSet().size, "Database file names must be unique")
    }

    @Test
    fun `the standard per-category configurations have db extension`() {
        representativeModes.forEach { mode ->
            val option = DatabaseOption.fromDataMode(mode)
            assert(option.fileName.endsWith(".db")) {
                "${option.category} fileName must end with .db but was: ${option.fileName}"
            }
        }
    }

    @Test
    fun `createDatabase returns same instance for same option on repeated calls`() {
        val factory = DatabaseFactory()
        val first = factory.createDatabase(DatabaseOption.OFFLINE)
        val second = factory.createDatabase(DatabaseOption.OFFLINE)
        assertSame(first, second, "DatabaseFactory must return the same instance for the same option")
    }

    @Test
    fun `createDatabase returns different instances for different options`() {
        val factory = DatabaseFactory()
        val none = factory.createDatabase(DatabaseOption.OFFLINE)
        val mock = factory.createDatabase(DatabaseOption.fromDataMode(DataMode.Mock))
        assertNotSame(none, mock, "DatabaseFactory must return different instances for different options")
    }

    @Test
    fun `evict causes next createDatabase to return a new instance`() {
        val factory = DatabaseFactory()
        val first = factory.createDatabase(DatabaseOption.OFFLINE)
        factory.evict(DatabaseOption.OFFLINE)
        val second = factory.createDatabase(DatabaseOption.OFFLINE)
        assertNotSame(first, second, "After evict, createDatabase must return a new instance")
    }

    @Test
    fun `legacy file names must not change without migration`() {
        assertEquals("battery-butler.db", DatabaseOption.legacyFileNames[DatabaseCategory.Offline])
        assertEquals("battery-butler-dev.db", DatabaseOption.legacyFileNames[DatabaseCategory.Mock])
    }

    @Test
    fun `only Offline and Mock have legacy file names`() {
        assertEquals(2, DatabaseOption.legacyFileNames.size)
        assertNull(DatabaseOption.legacyFileNames[DatabaseCategory.LocalServer])
        assertNull(DatabaseOption.legacyFileNames[DatabaseCategory.ProductionServer])
        assertNull(DatabaseOption.legacyFileNames[DatabaseCategory.DevServer])
    }

    @Test
    fun `fromDataMode maps all DataMode variants to the right category`() {
        assertEquals(DatabaseCategory.Offline, DatabaseOption.fromDataMode(DataMode.None).category)
        assertEquals(DatabaseCategory.Mock, DatabaseOption.fromDataMode(DataMode.Mock).category)
        assertEquals(
            DatabaseCategory.LocalServer,
            DatabaseOption.fromDataMode(DataMode.GrpcLocal("http://localhost:50051")).category,
        )
        assertEquals(
            DatabaseCategory.ProductionServer,
            DatabaseOption.fromDataMode(DataMode.GrpcAws("http://prod:443")).category,
        )
        assertEquals(
            DatabaseCategory.DevServer,
            DatabaseOption.fromDataMode(DataMode.GrpcDev("http://dev:443")).category,
        )
    }

    @Test
    fun `each standard configuration gets its own cached instance`() {
        val factory = DatabaseFactory()
        val instances = representativeModes.map { factory.createDatabase(DatabaseOption.fromDataMode(it)) }
        val uniqueInstances = instances.toSet()
        assertSame(
            instances.size,
            uniqueInstances.size,
            "Each standard configuration must produce a unique database instance",
        )
    }
}
