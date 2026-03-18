package com.chriscartland.batterybutler.datalocal.preferences

import kotlin.test.Test
import kotlin.test.assertSame

class DataStoreFactoryTest {
    @Test
    fun `createPreferencesDataStore returns same instance on repeated calls`() {
        val factory = DataStoreFactory()
        val first = factory.createPreferencesDataStore()
        val second = factory.createPreferencesDataStore()
        assertSame(first, second, "DataStoreFactory must return the same DataStore instance on every call")
    }
}
