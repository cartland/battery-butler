package com.chriscartland.batterybutler.datalocal.room

import com.chriscartland.batterybutler.domain.model.NetworkMode
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertNotEquals
import kotlin.test.assertNull

class DatabaseOptionTest {
    @Test
    fun `same mode constructed twice resolves to an equal DatabaseOption`() {
        val a = DatabaseOption.fromNetworkMode(NetworkMode.GrpcAws("https://prod.example.com"))
        val b = DatabaseOption.fromNetworkMode(NetworkMode.GrpcAws("https://prod.example.com"))
        assertEquals(a, b)
    }

    @Test
    fun `same subtype with a different url resolves to a different DatabaseOption and file name`() {
        val a = DatabaseOption.fromNetworkMode(NetworkMode.GrpcAws("https://prod-a.example.com"))
        val b = DatabaseOption.fromNetworkMode(NetworkMode.GrpcAws("https://prod-b.example.com"))
        assertNotEquals(a, b)
        assertNotEquals(a.fileName, b.fileName)
        // This is the exact bug this class exists to prevent: two different real backends must
        // never collapse onto the same category with the same file name.
        assertEquals(a.category, b.category)
    }

    @Test
    fun `null and blank urls resolve to the bare category file, with no suffix`() {
        val nullUrl = DatabaseOption.fromNetworkMode(NetworkMode.GrpcAws(null))
        val blankUrl = DatabaseOption.fromNetworkMode(NetworkMode.GrpcAws("   "))
        val bare = DatabaseOption.baseFileNames.getValue(DatabaseCategory.ProductionServer)
        assertEquals(bare, nullUrl.fileName)
        assertEquals(bare, blankUrl.fileName)
        assertEquals(nullUrl, blankUrl)
    }

    @Test
    fun `a literal 'null' string url does not collide with an actual null url`() {
        val nullUrl = DatabaseOption.fromNetworkMode(NetworkMode.GrpcAws(null))
        val literalNullString = DatabaseOption.fromNetworkMode(NetworkMode.GrpcAws("null"))
        assertNotEquals(nullUrl, literalNullString)
        assertNotEquals(DatabaseOption.baseFileNames.getValue(DatabaseCategory.ProductionServer), literalNullString.fileName)
    }

    @Test
    fun `case and trailing-slash-only url differences normalize to the same key`() {
        val a = DatabaseOption.fromNetworkMode(NetworkMode.GrpcDev("http://Example.com:8080/"))
        val b = DatabaseOption.fromNetworkMode(NetworkMode.GrpcDev("http://example.com:8080"))
        assertEquals(a, b)
    }

    @Test
    fun `every category's base file name matches the pre-existing locked literal`() {
        assertEquals("battery-butler-offline.db", DatabaseOption.baseFileNames.getValue(DatabaseCategory.Offline))
        assertEquals("battery-butler-mock.db", DatabaseOption.baseFileNames.getValue(DatabaseCategory.Mock))
        assertEquals("battery-butler-local-server.db", DatabaseOption.baseFileNames.getValue(DatabaseCategory.LocalServer))
        assertEquals("battery-butler-production-server.db", DatabaseOption.baseFileNames.getValue(DatabaseCategory.ProductionServer))
        assertEquals("battery-butler-dev-server.db", DatabaseOption.baseFileNames.getValue(DatabaseCategory.DevServer))
        assertEquals("battery-butler-labs-staging.db", DatabaseOption.baseFileNames.getValue(DatabaseCategory.LabsStaging))
        assertEquals("battery-butler-labs-prod.db", DatabaseOption.baseFileNames.getValue(DatabaseCategory.LabsProd))
    }

    @Test
    fun `legacy file names are still keyed correctly and unchanged`() {
        assertEquals("battery-butler.db", DatabaseOption.legacyFileNames[DatabaseCategory.Offline])
        assertEquals("battery-butler-dev.db", DatabaseOption.legacyFileNames[DatabaseCategory.Mock])
        assertNull(DatabaseOption.legacyFileNames[DatabaseCategory.LocalServer])
        assertNull(DatabaseOption.legacyFileNames[DatabaseCategory.ProductionServer])
        assertNull(DatabaseOption.legacyFileNames[DatabaseCategory.DevServer])
    }

    @Test
    fun `Mock and None ignore any url and resolve to their bare files`() {
        assertEquals(DatabaseOption.baseFileNames.getValue(DatabaseCategory.Mock), DatabaseOption.fromNetworkMode(NetworkMode.Mock).fileName)
        assertEquals(DatabaseOption.baseFileNames.getValue(DatabaseCategory.Offline), DatabaseOption.fromNetworkMode(NetworkMode.None).fileName)
        assertEquals(DatabaseOption.OFFLINE, DatabaseOption.fromNetworkMode(NetworkMode.None))
    }
}
