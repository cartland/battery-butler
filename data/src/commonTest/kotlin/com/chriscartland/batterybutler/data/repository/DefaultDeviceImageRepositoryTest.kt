package com.chriscartland.batterybutler.data.repository

import com.chriscartland.batterybutler.domain.model.DeviceImageBytes
import com.chriscartland.batterybutler.domain.model.DeviceImageError
import com.chriscartland.batterybutler.domain.model.Result
import com.chriscartland.batterybutler.testcommon.FakeDeviceImageCache
import com.chriscartland.batterybutler.testcommon.FakeDeviceImageDataSource
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.test.runTest
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertFalse
import kotlin.test.assertIs
import kotlin.test.assertNull
import kotlin.test.assertTrue

class DefaultDeviceImageRepositoryTest {
    @Test
    fun `supported delegates to the data source`() =
        runTest {
            val dataSource = FakeDeviceImageDataSource().apply { setSupported(true) }
            val repo = DefaultDeviceImageRepository(dataSource, FakeDeviceImageCache())

            assertTrue(repo.supported.first())
        }

    @Test
    fun `observeCachedImage delegates to the cache`() =
        runTest {
            val cache = FakeDeviceImageCache()
            val bytes = DeviceImageBytes(byteArrayOf(9, 8, 7), "image/png")
            cache.entries["etag-1"] = bytes
            val repo = DefaultDeviceImageRepository(FakeDeviceImageDataSource(), cache)

            val result = repo.observeCachedImage("etag-1").first()

            assertEquals(listOf<Byte>(9, 8, 7), result?.bytes?.toList())
            assertEquals("image/png", result?.contentType)
        }

    @Test
    fun `observeCachedImage emits null for an uncached etag`() =
        runTest {
            val repo = DefaultDeviceImageRepository(FakeDeviceImageDataSource(), FakeDeviceImageCache())

            assertNull(repo.observeCachedImage("missing").first())
        }

    @Test
    fun `uploadImage caches the bytes under the returned etag on success`() =
        runTest {
            val dataSource = FakeDeviceImageDataSource().apply {
                uploadResult = Result.Success("new-etag")
            }
            val cache = FakeDeviceImageCache()
            val repo = DefaultDeviceImageRepository(dataSource, cache)

            val result = repo.uploadImage("dev1", byteArrayOf(1, 2), "image/jpeg")

            assertIs<Result.Success<String>>(result)
            assertEquals("new-etag", result.data)
            assertEquals(listOf<Byte>(1, 2), cache.entries["new-etag"]?.bytes?.toList())
            assertEquals("image/jpeg", cache.entries["new-etag"]?.contentType)
        }

    @Test
    fun `uploadImage does not touch the cache on failure`() =
        runTest {
            val dataSource = FakeDeviceImageDataSource().apply {
                uploadResult = Result.Error(DeviceImageError.DeviceNotFound())
            }
            val cache = FakeDeviceImageCache()
            val repo = DefaultDeviceImageRepository(dataSource, cache)

            val result = repo.uploadImage("dev1", byteArrayOf(1, 2), "image/jpeg")

            assertIs<Result.Error<DeviceImageError>>(result)
            assertTrue(cache.entries.isEmpty())
        }

    @Test
    fun `deleteImage delegates to the data source`() =
        runTest {
            val dataSource = FakeDeviceImageDataSource().apply { deleteResult = false }
            val repo = DefaultDeviceImageRepository(dataSource, FakeDeviceImageCache())

            val result = repo.deleteImage("dev1")

            assertFalse(result)
            assertEquals(listOf("dev1"), dataSource.deletedDeviceIds)
        }
}
