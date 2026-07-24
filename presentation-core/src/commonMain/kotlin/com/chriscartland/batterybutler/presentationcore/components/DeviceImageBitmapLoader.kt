package com.chriscartland.batterybutler.presentationcore.components

import androidx.compose.ui.graphics.ImageBitmap
import co.touchlab.kermit.Logger
import kotlinx.coroutines.CancellationException
import kotlinx.coroutines.CoroutineDispatcher
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.withContext
import org.jetbrains.compose.resources.decodeToImageBitmap

/**
 * Decodes device photo bytes into [ImageBitmap]s off the calling thread, and caches the result by
 * [imageEtag] so a given cached photo is decoded at most once, no matter how many times its bytes
 * are re-fetched with a new [ByteArray] instance (e.g. Room re-querying after an unrelated change
 * elsewhere in the screen's state -- [ByteArray] equality is reference-based, so those re-fetches
 * would otherwise look like "new" images). [decode] is injectable so tests can avoid a real Skia
 * decode and assert caching/dispatch behavior directly.
 */
class DeviceImageBitmapLoader(
    private val decodeDispatcher: CoroutineDispatcher = Dispatchers.Default,
    private val maxCacheEntries: Int = MAX_CACHE_ENTRIES,
    private val decode: (ByteArray) -> ImageBitmap? = { it.decodeToImageBitmap() },
) {
    private val cache = MutableStateFlow<Map<String, ImageBitmap>>(emptyMap())

    /** Synchronous cache read, safe to call during composition to avoid a decode flicker. */
    fun peek(imageEtag: String): ImageBitmap? = cache.value[imageEtag]

    suspend fun load(
        imageEtag: String,
        bytes: ByteArray,
    ): ImageBitmap? {
        peek(imageEtag)?.let { return it }

        val bitmap = withContext(decodeDispatcher) {
            try {
                decode(bytes)
            } catch (e: CancellationException) {
                throw e
            } catch (e: Exception) {
                Logger.w(TAG, e) { "Failed to decode cached device image" }
                null
            }
        } ?: return null

        cache.update { current ->
            val trimmed = if (current.size >= maxCacheEntries) current - current.keys.first() else current
            trimmed + (imageEtag to bitmap)
        }
        return bitmap
    }

    companion object {
        private const val TAG = "DeviceImageBitmapLoader"
        private const val MAX_CACHE_ENTRIES = 30

        /** Shared across the app so a photo decoded on one screen is reused on every other. */
        val shared = DeviceImageBitmapLoader()
    }
}
