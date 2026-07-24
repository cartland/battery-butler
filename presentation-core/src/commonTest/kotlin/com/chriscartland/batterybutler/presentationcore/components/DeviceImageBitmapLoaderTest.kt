package com.chriscartland.batterybutler.presentationcore.components

import androidx.compose.ui.graphics.ImageBitmap
import androidx.compose.ui.graphics.ImageBitmapConfig
import androidx.compose.ui.graphics.colorspace.ColorSpace
import androidx.compose.ui.graphics.colorspace.ColorSpaces
import kotlinx.coroutines.CancellationException
import kotlinx.coroutines.CoroutineStart
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.launch
import kotlinx.coroutines.test.StandardTestDispatcher
import kotlinx.coroutines.test.runTest
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertFailsWith
import kotlin.test.assertNotNull
import kotlin.test.assertNull
import kotlin.test.assertSame

/**
 * A no-op [ImageBitmap] that never touches a real (Skia/Android) decoder, so these tests can run
 * as plain JVM unit tests without native graphics libraries on the classpath.
 */
private class FakeImageBitmap : ImageBitmap {
    override val width: Int = 1
    override val height: Int = 1
    override val colorSpace: ColorSpace = ColorSpaces.Srgb
    override val hasAlpha: Boolean = false
    override val config: ImageBitmapConfig = ImageBitmapConfig.Argb8888

    override fun readPixels(
        buffer: IntArray,
        startX: Int,
        startY: Int,
        width: Int,
        height: Int,
        bufferOffset: Int,
        stride: Int,
    ) = Unit

    override fun prepareToDraw() = Unit
}

@OptIn(ExperimentalCoroutinesApi::class)
class DeviceImageBitmapLoaderTest {
    private val fakeBitmap: ImageBitmap = FakeImageBitmap()

    @Test
    fun `decodes via the injected dispatcher and returns the decoded bitmap`() =
        runTest {
            val dispatcher = StandardTestDispatcher(testScheduler)
            var decodeCalls = 0
            val loader = DeviceImageBitmapLoader(decodeDispatcher = dispatcher, decode = {
                decodeCalls++
                fakeBitmap
            })

            val result = loader.load("etag-1", byteArrayOf(1, 2, 3))

            assertSame(fakeBitmap, result)
            assertEquals(1, decodeCalls)
        }

    /**
     * This is the actual bug that motivated this class: [rememberDeviceImageBitmap] used to call
     * `decodeToImageBitmap()` directly inside a Compose `remember{}` block -- on the composition
     * (main) thread, with no dispatcher involved at all. Proves the fix by launching [load]
     * undispatched and asserting decode has *not* run until the decode dispatcher is pumped --
     * if [load] ever goes back to calling [decode] inline, this fails immediately since
     * `decodeCalls` would already be 1 before the dispatcher runs anything.
     */
    @Test
    fun `load suspends at the decode dispatcher instead of decoding inline on the caller`() =
        runTest {
            val dispatcher = StandardTestDispatcher(testScheduler)
            var decodeCalls = 0
            val loader = DeviceImageBitmapLoader(decodeDispatcher = dispatcher, decode = {
                decodeCalls++
                fakeBitmap
            })

            var result: ImageBitmap? = null
            launch(start = CoroutineStart.UNDISPATCHED) {
                result = loader.load("etag-1", byteArrayOf(1, 2, 3))
            }

            assertEquals(0, decodeCalls)
            assertNull(result)

            dispatcher.scheduler.advanceUntilIdle()

            assertEquals(1, decodeCalls)
            assertSame(fakeBitmap, result)
        }

    @Test
    fun `caches by etag so a second load with a new byte array instance does not decode again`() =
        runTest {
            var decodeCalls = 0
            val loader = DeviceImageBitmapLoader(decode = {
                decodeCalls++
                fakeBitmap
            })

            loader.load("etag-1", byteArrayOf(1, 2, 3))
            loader.load("etag-1", byteArrayOf(1, 2, 3)) // a distinct ByteArray instance, same etag

            assertEquals(1, decodeCalls)
        }

    @Test
    fun `different etags decode independently`() =
        runTest {
            var decodeCalls = 0
            val loader = DeviceImageBitmapLoader(decode = {
                decodeCalls++
                fakeBitmap
            })

            loader.load("etag-1", byteArrayOf(1))
            loader.load("etag-2", byteArrayOf(2))

            assertEquals(2, decodeCalls)
        }

    @Test
    fun `peek returns null before load and the cached bitmap after`() =
        runTest {
            val loader = DeviceImageBitmapLoader(decode = { fakeBitmap })

            assertNull(loader.peek("etag-1"))
            loader.load("etag-1", byteArrayOf(1))
            assertSame(fakeBitmap, loader.peek("etag-1"))
        }

    @Test
    fun `evicts the oldest entry once the cache exceeds its max size`() =
        runTest {
            val loader = DeviceImageBitmapLoader(maxCacheEntries = 2, decode = { fakeBitmap })

            loader.load("etag-1", byteArrayOf(1))
            loader.load("etag-2", byteArrayOf(2))
            loader.load("etag-3", byteArrayOf(3))

            assertNull(loader.peek("etag-1"))
            assertNotNull(loader.peek("etag-2"))
            assertNotNull(loader.peek("etag-3"))
        }

    @Test
    fun `returns null and does not cache when decoding fails`() =
        runTest {
            val loader = DeviceImageBitmapLoader(decode = { throw IllegalStateException("corrupt") })

            val result = loader.load("etag-1", byteArrayOf(1))

            assertNull(result)
            assertNull(loader.peek("etag-1"))
        }

    @Test
    fun `propagates cancellation instead of swallowing it as a decode failure`() =
        runTest {
            val loader = DeviceImageBitmapLoader(decode = { throw CancellationException("cancelled") })

            assertFailsWith<CancellationException> {
                loader.load("etag-1", byteArrayOf(1))
            }
        }
}
