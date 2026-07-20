package com.chriscartland.batterybutler.datalocal

import com.chriscartland.batterybutler.datalocal.room.DatabaseFactory
import com.chriscartland.batterybutler.datalocal.room.DatabaseOption
import com.chriscartland.batterybutler.domain.model.DeviceImageBytes
import kotlinx.coroutines.test.runTest
import java.io.File
import kotlin.test.AfterTest
import kotlin.test.BeforeTest
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertNull
import kotlin.test.assertTrue

class RoomDeviceImageCacheTest {
    private val tmpDir = File(System.getProperty("java.io.tmpdir"))

    @BeforeTest
    @AfterTest
    fun cleanDbFiles() {
        DatabaseOption.baseFileNames.values.forEach { File(tmpDir, it).delete() }
    }

    private fun cache(): RoomDeviceImageCache = RoomDeviceImageCache(DatabaseFactory().createDatabase())

    @Test
    fun `get returns null for an uncached etag`() =
        runTest {
            assertNull(cache().get("missing"))
        }

    @Test
    fun `put then get round-trips bytes and content type`() =
        runTest {
            val store = cache()
            store.put("etag-1", DeviceImageBytes(byteArrayOf(1, 2, 3), "image/jpeg"))

            val result = store.get("etag-1")

            assertEquals(listOf<Byte>(1, 2, 3), result?.bytes?.toList())
            assertEquals("image/jpeg", result?.contentType)
        }

    @Test
    fun `put overwrites an existing entry for the same etag`() =
        runTest {
            val store = cache()
            store.put("etag-1", DeviceImageBytes(byteArrayOf(1), "image/jpeg"))
            store.put("etag-1", DeviceImageBytes(byteArrayOf(2), "image/png"))

            val result = store.get("etag-1")

            assertEquals(listOf<Byte>(2), result?.bytes?.toList())
            assertEquals("image/png", result?.contentType)
        }

    @Test
    fun `evictExcept drops entries not in the keep set`() =
        runTest {
            val store = cache()
            store.put("keep", DeviceImageBytes(byteArrayOf(1), "image/jpeg"))
            store.put("drop", DeviceImageBytes(byteArrayOf(2), "image/jpeg"))

            store.evictExcept(setOf("keep"))

            assertTrue(store.get("keep") != null)
            assertNull(store.get("drop"))
        }

    @Test
    fun `evictExcept with an empty keep set drops everything`() =
        runTest {
            val store = cache()
            store.put("a", DeviceImageBytes(byteArrayOf(1), "image/jpeg"))
            store.put("b", DeviceImageBytes(byteArrayOf(2), "image/jpeg"))

            store.evictExcept(emptySet())

            assertNull(store.get("a"))
            assertNull(store.get("b"))
        }
}
