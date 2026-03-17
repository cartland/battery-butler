package com.chriscartland.batterybutler.datalocal.room

import kotlin.test.Test
import kotlin.test.assertNotSame
import kotlin.test.assertSame

class DatabaseFactoryTest {
    @Test
    fun `createDatabase returns same instance for default name on repeated calls`() {
        val factory = DatabaseFactory()
        val first = factory.createDatabase()
        val second = factory.createDatabase()
        assertSame(first, second, "DatabaseFactory must return the same instance for the default database name")
    }

    @Test
    fun `createDatabase returns different instance for non-default name`() {
        val factory = DatabaseFactory()
        val default = factory.createDatabase()
        val other = factory.createDatabase("other.db")
        assertNotSame(default, other, "DatabaseFactory must return a different instance for a non-default name")
    }
}
