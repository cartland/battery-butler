package com.chriscartland.batterybutler.presentationcore.util

import java.awt.Color
import java.awt.image.BufferedImage
import java.io.ByteArrayInputStream
import java.io.ByteArrayOutputStream
import javax.imageio.ImageIO
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertNotNull
import kotlin.test.assertNull
import kotlin.test.assertTrue

class DeviceImageNormalizerTest {
    @Test
    fun `normalizes a small opaque JPEG to a decodable JPEG`() {
        val input = encode(solidImage(100, 80, Color.RED), "jpg")

        val result = normalizeDeviceImage(input)

        assertNotNull(result)
        assertEquals("image/jpeg", result.contentType)
        val decoded = ImageIO.read(ByteArrayInputStream(result.bytes))
        assertNotNull(decoded)
        assertEquals(100, decoded.width)
        assertEquals(80, decoded.height)
    }

    @Test
    fun `flattens a transparent PNG onto an opaque background instead of corrupting it`() {
        val transparent = BufferedImage(50, 50, BufferedImage.TYPE_INT_ARGB)
        // Fully transparent -- if flattening didn't happen, JPEG encoding would misrender this.
        val input = encode(transparent, "png")

        val result = normalizeDeviceImage(input)

        assertNotNull(result)
        val decoded = ImageIO.read(ByteArrayInputStream(result.bytes))
        assertNotNull(decoded)
        assertEquals(50, decoded.width)
        assertEquals(50, decoded.height)
    }

    @Test
    fun `downscales an oversized image so the long edge is at most the max dimension`() {
        val input = encode(solidImage(4000, 2000, Color.BLUE), "png")

        val result = normalizeDeviceImage(input)

        assertNotNull(result)
        val decoded = ImageIO.read(ByteArrayInputStream(result.bytes))
        assertNotNull(decoded)
        assertTrue(decoded.width <= DEVICE_IMAGE_MAX_DIMENSION_PX)
        assertTrue(decoded.height <= DEVICE_IMAGE_MAX_DIMENSION_PX)
        // Aspect ratio preserved (2:1)
        assertEquals(decoded.width, decoded.height * 2)
    }

    @Test
    fun `does not upscale an image already under the max dimension`() {
        val input = encode(solidImage(200, 100, Color.GREEN), "png")

        val result = normalizeDeviceImage(input)

        assertNotNull(result)
        val decoded = ImageIO.read(ByteArrayInputStream(result.bytes))
        assertNotNull(decoded)
        assertEquals(200, decoded.width)
        assertEquals(100, decoded.height)
    }

    @Test
    fun `returns null for bytes that aren't a decodable image`() {
        val result = normalizeDeviceImage("not an image".encodeToByteArray())
        assertNull(result)
    }

    @Test
    fun `returns null for empty bytes`() {
        assertNull(normalizeDeviceImage(ByteArray(0)))
    }

    private fun solidImage(
        width: Int,
        height: Int,
        color: Color,
    ): BufferedImage {
        val image = BufferedImage(width, height, BufferedImage.TYPE_INT_RGB)
        val g = image.createGraphics()
        g.color = color
        g.fillRect(0, 0, width, height)
        g.dispose()
        return image
    }

    private fun encode(
        image: BufferedImage,
        format: String,
    ): ByteArray {
        val output = ByteArrayOutputStream()
        ImageIO.write(image, format, output)
        return output.toByteArray()
    }
}
