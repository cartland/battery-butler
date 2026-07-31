package com.chriscartland.batterybutler.data.repository

import com.chriscartland.batterybutler.domain.model.Device
import com.chriscartland.batterybutler.domain.model.DeviceImageBytes
import com.chriscartland.batterybutler.domain.repository.RemoteUpdate
import com.chriscartland.batterybutler.testcommon.FakeDeviceImageCache
import com.chriscartland.batterybutler.testcommon.FakeDeviceImageDataSource
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.Job
import kotlinx.coroutines.test.StandardTestDispatcher
import kotlinx.coroutines.test.advanceUntilIdle
import kotlinx.coroutines.test.runTest
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertNull
import kotlin.test.assertTrue
import kotlin.time.Instant

@OptIn(ExperimentalCoroutinesApi::class)
class DeviceImageSyncCoordinatorTest {
    private fun device(
        id: String,
        imageEtag: String? = null,
    ): Device =
        Device(
            id = id,
            name = "Device $id",
            typeId = "type-1",
            batteryLastReplaced = Instant.DISTANT_PAST,
            lastUpdated = Instant.DISTANT_PAST,
            imageEtag = imageEtag,
        )

    @Test
    fun `does nothing when the backend doesn't support images`() =
        runTest {
            val dispatcher = StandardTestDispatcher(testScheduler)
            val images = FakeDeviceImageDataSource() // supported = false by default
            val cache = FakeDeviceImageCache()
            val coordinator = DeviceImageSyncCoordinator(images, cache, CoroutineScope(dispatcher + Job()))

            coordinator.onRemoteUpdate(
                RemoteUpdate(isFullSnapshot = true, deviceTypes = emptyList(), devices = listOf(device("d1", "etag-1")), events = emptyList()),
            )
            advanceUntilIdle()

            assertTrue(images.fetchedDeviceIds.isEmpty())
            assertTrue(cache.entries.isEmpty())
        }

    @Test
    fun `fetches and caches images for devices with an etag not already cached`() =
        runTest {
            val dispatcher = StandardTestDispatcher(testScheduler)
            val images = FakeDeviceImageDataSource().apply { setSupported(true) }
            images.uploadedBytes["d1"] = DeviceImageBytes(byteArrayOf(1, 2, 3), "image/jpeg")
            val cache = FakeDeviceImageCache()
            val coordinator = DeviceImageSyncCoordinator(images, cache, CoroutineScope(dispatcher + Job()))

            coordinator.onRemoteUpdate(
                RemoteUpdate(isFullSnapshot = true, deviceTypes = emptyList(), devices = listOf(device("d1", "etag-1")), events = emptyList()),
            )
            advanceUntilIdle()

            assertEquals(listOf("d1"), images.fetchedDeviceIds)
            assertEquals(listOf<Byte>(1, 2, 3), cache.entries["etag-1"]?.bytes?.toList())
        }

    @Test
    fun `skips devices with no imageEtag`() =
        runTest {
            val dispatcher = StandardTestDispatcher(testScheduler)
            val images = FakeDeviceImageDataSource().apply { setSupported(true) }
            val cache = FakeDeviceImageCache()
            val coordinator = DeviceImageSyncCoordinator(images, cache, CoroutineScope(dispatcher + Job()))

            coordinator.onRemoteUpdate(
                RemoteUpdate(isFullSnapshot = true, deviceTypes = emptyList(), devices = listOf(device("d1", imageEtag = null)), events = emptyList()),
            )
            advanceUntilIdle()

            assertTrue(images.fetchedDeviceIds.isEmpty())
        }

    @Test
    fun `does not re-fetch an etag that's already cached`() =
        runTest {
            val dispatcher = StandardTestDispatcher(testScheduler)
            val images = FakeDeviceImageDataSource().apply { setSupported(true) }
            val cache = FakeDeviceImageCache()
            cache.entries["etag-1"] = DeviceImageBytes(byteArrayOf(9), "image/jpeg")
            val coordinator = DeviceImageSyncCoordinator(images, cache, CoroutineScope(dispatcher + Job()))

            coordinator.onRemoteUpdate(
                RemoteUpdate(isFullSnapshot = true, deviceTypes = emptyList(), devices = listOf(device("d1", "etag-1")), events = emptyList()),
            )
            advanceUntilIdle()

            assertTrue(images.fetchedDeviceIds.isEmpty())
        }

    @Test
    fun `a full snapshot evicts cache entries no longer referenced by any device`() =
        runTest {
            val dispatcher = StandardTestDispatcher(testScheduler)
            val images = FakeDeviceImageDataSource().apply { setSupported(true) }
            val cache = FakeDeviceImageCache()
            cache.entries["stale-etag"] = DeviceImageBytes(byteArrayOf(9), "image/jpeg")
            cache.entries["current-etag"] = DeviceImageBytes(byteArrayOf(1), "image/jpeg")
            val coordinator = DeviceImageSyncCoordinator(images, cache, CoroutineScope(dispatcher + Job()))

            coordinator.onRemoteUpdate(
                RemoteUpdate(isFullSnapshot = true, deviceTypes = emptyList(), devices = listOf(device("d1", "current-etag")), events = emptyList()),
            )
            advanceUntilIdle()

            assertNull(cache.entries["stale-etag"])
            assertTrue(cache.entries.containsKey("current-etag"))
        }

    @Test
    fun `a partial non-full-snapshot update does not evict`() =
        runTest {
            val dispatcher = StandardTestDispatcher(testScheduler)
            val images = FakeDeviceImageDataSource().apply { setSupported(true) }
            val cache = FakeDeviceImageCache()
            cache.entries["untouched-etag"] = DeviceImageBytes(byteArrayOf(9), "image/jpeg")
            val coordinator = DeviceImageSyncCoordinator(images, cache, CoroutineScope(dispatcher + Job()))

            coordinator.onRemoteUpdate(
                RemoteUpdate(isFullSnapshot = false, deviceTypes = emptyList(), devices = emptyList(), events = emptyList()),
            )
            advanceUntilIdle()

            assertTrue(cache.entries.containsKey("untouched-etag"))
        }

    @Test
    fun `a failed fetch does not populate the cache and does not throw`() =
        runTest {
            val dispatcher = StandardTestDispatcher(testScheduler)
            val images = FakeDeviceImageDataSource().apply { setSupported(true) } // no bytes registered -> fetch returns null
            val cache = FakeDeviceImageCache()
            val coordinator = DeviceImageSyncCoordinator(images, cache, CoroutineScope(dispatcher + Job()))

            coordinator.onRemoteUpdate(
                RemoteUpdate(isFullSnapshot = true, deviceTypes = emptyList(), devices = listOf(device("d1", "etag-1")), events = emptyList()),
            )
            advanceUntilIdle()

            assertTrue(cache.entries.isEmpty())
        }
}
