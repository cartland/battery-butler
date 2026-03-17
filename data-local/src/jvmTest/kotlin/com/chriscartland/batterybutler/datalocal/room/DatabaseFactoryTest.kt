package com.chriscartland.batterybutler.datalocal.room

import kotlin.test.Test
import kotlin.test.assertNotSame
import kotlin.test.assertSame

class DatabaseFactoryTest {
    @Test
    fun `createDatabase returns same instance for same option on repeated calls`() {
        val factory = DatabaseFactory()
        val first = factory.createDatabase(DatabaseOption.None)
        val second = factory.createDatabase(DatabaseOption.None)
        assertSame(first, second, "DatabaseFactory must return the same instance for the same option")
    }

    @Test
    fun `createDatabase returns different instances for different options`() {
        val factory = DatabaseFactory()
        val none = factory.createDatabase(DatabaseOption.None)
        val mock = factory.createDatabase(DatabaseOption.Mock)
        assertNotSame(none, mock, "DatabaseFactory must return different instances for different options")
    }

    @Test
    fun `evict causes next createDatabase to return a new instance`() {
        val factory = DatabaseFactory()
        val first = factory.createDatabase(DatabaseOption.None)
        factory.evict(DatabaseOption.None)
        val second = factory.createDatabase(DatabaseOption.None)
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
