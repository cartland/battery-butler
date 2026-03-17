package com.chriscartland.batterybutler.datalocal.room

import kotlin.test.Test
import kotlin.test.assertNotSame
import kotlin.test.assertSame

class DatabaseFactoryTest {
    @Test
    fun `createDatabase returns same instance for Production on repeated calls`() {
        val factory = DatabaseFactory()
        val first = factory.createDatabase(DatabaseOption.Production)
        val second = factory.createDatabase(DatabaseOption.Production)
        assertSame(first, second, "DatabaseFactory must return the same instance for Production")
    }

    @Test
    fun `createDatabase returns same instance for Development on repeated calls`() {
        val factory = DatabaseFactory()
        val first = factory.createDatabase(DatabaseOption.Development)
        val second = factory.createDatabase(DatabaseOption.Development)
        assertSame(first, second, "DatabaseFactory must return the same instance for Development")
    }

    @Test
    fun `createDatabase returns different instances for different options`() {
        val factory = DatabaseFactory()
        val production = factory.createDatabase(DatabaseOption.Production)
        val development = factory.createDatabase(DatabaseOption.Development)
        assertNotSame(production, development, "DatabaseFactory must return different instances for different options")
    }

    @Test
    fun `evict causes next createDatabase to return a new instance`() {
        val factory = DatabaseFactory()
        val first = factory.createDatabase(DatabaseOption.Production)
        factory.evict(DatabaseOption.Production)
        val second = factory.createDatabase(DatabaseOption.Production)
        assertNotSame(first, second, "After evict, createDatabase must return a new instance")
    }
}
