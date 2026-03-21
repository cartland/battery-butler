package com.chriscartland.batterybutler.datalocal.room

import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertNotSame
import kotlin.test.assertSame

class DatabaseFactoryTest {
    /**
     * LOCKED DATABASE FILE NAMES — DO NOT CHANGE WITHOUT MIGRATION.
     *
     * These file names are persisted on user devices. Changing a file name
     * without adding migration logic (renaming the old file to the new name
     * BEFORE Room opens it) will cause users to lose all local data.
     *
     * If you need to rename a database file:
     * 1. Add migration code in DatabaseFactory (each platform) that renames
     *    the old file to the new name before createDatabase() is called.
     * 2. Update this test to reflect the new file name.
     * 3. Add the old file name to the migration logic so it is handled.
     */
    @Test
    fun `database file names must not change without migration`() {
        assertEquals("battery-butler-offline.db", DatabaseOption.Offline.fileName)
        assertEquals("battery-butler-mock.db", DatabaseOption.Mock.fileName)
        assertEquals("battery-butler-local-server.db", DatabaseOption.LocalServer.fileName)
        assertEquals("battery-butler-production-server.db", DatabaseOption.ProductionServer.fileName)
        assertEquals("battery-butler-dev-server.db", DatabaseOption.DevServer.fileName)
    }

    @Test
    fun `all DatabaseOption entries have unique file names`() {
        val fileNames = DatabaseOption.entries.map { it.fileName }
        assertEquals(fileNames.size, fileNames.toSet().size, "Database file names must be unique")
    }

    @Test
    fun `all DatabaseOption entries have db extension`() {
        DatabaseOption.entries.forEach { option ->
            assert(option.fileName.endsWith(".db")) {
                "${option.name} fileName must end with .db but was: ${option.fileName}"
            }
        }
    }

    @Test
    fun `createDatabase returns same instance for same option on repeated calls`() {
        val factory = DatabaseFactory()
        val first = factory.createDatabase(DatabaseOption.Offline)
        val second = factory.createDatabase(DatabaseOption.Offline)
        assertSame(first, second, "DatabaseFactory must return the same instance for the same option")
    }

    @Test
    fun `createDatabase returns different instances for different options`() {
        val factory = DatabaseFactory()
        val none = factory.createDatabase(DatabaseOption.Offline)
        val mock = factory.createDatabase(DatabaseOption.Mock)
        assertNotSame(none, mock, "DatabaseFactory must return different instances for different options")
    }

    @Test
    fun `evict causes next createDatabase to return a new instance`() {
        val factory = DatabaseFactory()
        val first = factory.createDatabase(DatabaseOption.Offline)
        factory.evict(DatabaseOption.Offline)
        val second = factory.createDatabase(DatabaseOption.Offline)
        assertNotSame(first, second, "After evict, createDatabase must return a new instance")
    }

    @Test
    fun `each DatabaseOption gets its own cached instance`() {
        val factory = DatabaseFactory()
        val instances = DatabaseOption.entries.map { factory.createDatabase(it) }
        val uniqueInstances = instances.toSet()
        assertSame(
            instances.size,
            uniqueInstances.size,
            "Each DatabaseOption must produce a unique database instance",
        )
    }
}
