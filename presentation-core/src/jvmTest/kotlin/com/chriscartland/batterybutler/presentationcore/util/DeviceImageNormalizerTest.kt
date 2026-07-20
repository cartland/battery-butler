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

    // --- EXIF orientation ---
    //
    // A 40x24 (landscape) image with a distinct solid color per quadrant lets each orientation's
    // corner-mapping be verified empirically by sampling a point well inside each quadrant
    // (avoiding JPEG block-boundary artifacts), rather than trusting the transform math by
    // inspection -- exactly the kind of rotation/flip logic that's easy to get subtly backwards.

    @Test
    fun `orientation 6 (rotate 90 CW) -- landscape becomes portrait, top-left moves to top-right`() {
        val input = injectExifOrientation(encode(quadrantImage(40, 24), "jpg"), orientation = 6)

        val result = normalizeDeviceImage(input)

        assertNotNull(result)
        val decoded = ImageIO.read(ByteArrayInputStream(result.bytes))
        assertNotNull(decoded)
        assertEquals(24, decoded.width)
        assertEquals(40, decoded.height)
        // Source top-left (RED) -> destination top-right; source bottom-left (BLUE) -> top-left.
        assertColorAt(decoded, decoded.width * 3 / 4, decoded.height / 8, Color.RED)
        assertColorAt(decoded, decoded.width / 8, decoded.height / 8, Color.BLUE)
    }

    @Test
    fun `orientation 8 (rotate 270 CW) -- landscape becomes portrait, top-left moves to bottom-left`() {
        val input = injectExifOrientation(encode(quadrantImage(40, 24), "jpg"), orientation = 8)

        val result = normalizeDeviceImage(input)

        assertNotNull(result)
        val decoded = ImageIO.read(ByteArrayInputStream(result.bytes))
        assertNotNull(decoded)
        assertEquals(24, decoded.width)
        assertEquals(40, decoded.height)
        // Source top-left (RED) -> destination bottom-left; source top-right (GREEN) -> top-left.
        assertColorAt(decoded, decoded.width / 8, decoded.height * 7 / 8, Color.RED)
        assertColorAt(decoded, decoded.width / 8, decoded.height / 8, Color.GREEN)
    }

    @Test
    fun `orientation 3 (rotate 180) -- same dimensions, corners swap diagonally`() {
        val input = injectExifOrientation(encode(quadrantImage(40, 24), "jpg"), orientation = 3)

        val result = normalizeDeviceImage(input)

        assertNotNull(result)
        val decoded = ImageIO.read(ByteArrayInputStream(result.bytes))
        assertNotNull(decoded)
        assertEquals(40, decoded.width)
        assertEquals(24, decoded.height)
        // Source top-left (RED) -> destination bottom-right; source bottom-right (YELLOW) -> top-left.
        assertColorAt(decoded, decoded.width * 7 / 8, decoded.height * 7 / 8, Color.RED)
        assertColorAt(decoded, decoded.width / 8, decoded.height / 8, Color.YELLOW)
    }

    @Test
    fun `orientation 2 (flip horizontal) -- left and right halves swap`() {
        val input = injectExifOrientation(encode(quadrantImage(40, 24), "jpg"), orientation = 2)

        val result = normalizeDeviceImage(input)

        assertNotNull(result)
        val decoded = ImageIO.read(ByteArrayInputStream(result.bytes))
        assertNotNull(decoded)
        assertEquals(40, decoded.width)
        assertEquals(24, decoded.height)
        // Source top-left (RED) -> destination top-right.
        assertColorAt(decoded, decoded.width * 7 / 8, decoded.height / 8, Color.RED)
        assertColorAt(decoded, decoded.width / 8, decoded.height / 8, Color.GREEN)
    }

    @Test
    fun `orientation 1 (normal) and no EXIF at all leave the image untouched`() {
        val plain = encode(quadrantImage(40, 24), "jpg")
        val explicitNormal = injectExifOrientation(plain, orientation = 1)

        for (input in listOf(plain, explicitNormal)) {
            val result = normalizeDeviceImage(input)
            assertNotNull(result)
            val decoded = ImageIO.read(ByteArrayInputStream(result.bytes))
            assertNotNull(decoded)
            assertColorAt(decoded, decoded.width / 8, decoded.height / 8, Color.RED)
            assertColorAt(decoded, decoded.width * 7 / 8, decoded.height / 8, Color.GREEN)
        }
    }

    /** Distinct solid color per quadrant: top-left RED, top-right GREEN, bottom-left BLUE, bottom-right YELLOW. */
    private fun quadrantImage(
        width: Int,
        height: Int,
    ): BufferedImage {
        val image = BufferedImage(width, height, BufferedImage.TYPE_INT_RGB)
        val g = image.createGraphics()
        val halfW = width / 2
        val halfH = height / 2
        g.color = Color.RED
        g.fillRect(0, 0, halfW, halfH)
        g.color = Color.GREEN
        g.fillRect(halfW, 0, width - halfW, halfH)
        g.color = Color.BLUE
        g.fillRect(0, halfH, halfW, height - halfH)
        g.color = Color.YELLOW
        g.fillRect(halfW, halfH, width - halfW, height - halfH)
        g.dispose()
        return image
    }

    /** Tolerant color check (JPEG is lossy) -- asserts [expected]'s dominant channel(s) also dominate at [x], [y]. */
    private fun assertColorAt(
        image: BufferedImage,
        x: Int,
        y: Int,
        expected: Color,
    ) {
        val actual = Color(image.getRGB(x, y))
        val tolerance = 40
        assertTrue(
            kotlin.math.abs(actual.red - expected.red) <= tolerance &&
                kotlin.math.abs(actual.green - expected.green) <= tolerance &&
                kotlin.math.abs(actual.blue - expected.blue) <= tolerance,
            "Expected ~$expected at ($x,$y) but was $actual",
        )
    }

    /**
     * Splices a minimal JPEG APP1/Exif segment (TIFF header + a single IFD0 Orientation entry,
     * little-endian) right after the SOI marker of [jpegBytes] -- real cameras write far more
     * metadata, but a decoder (and this normalizer's own parser) only needs this much to find the
     * orientation tag.
     */
    private fun injectExifOrientation(
        jpegBytes: ByteArray,
        orientation: Int,
    ): ByteArray {
        val tiff = byteArrayOf(
            'I'.code.toByte(),
            'I'.code.toByte(), // little-endian
            0x2A,
            0x00, // TIFF magic
            0x08,
            0x00,
            0x00,
            0x00, // IFD0 offset = 8
            0x01,
            0x00, // 1 entry
            0x12,
            0x01, // tag 0x0112 = Orientation
            0x03,
            0x00, // type 3 = SHORT
            0x01,
            0x00,
            0x00,
            0x00, // count = 1
            orientation.toByte(),
            0x00,
            0x00,
            0x00, // value (+ 2 bytes padding)
            0x00,
            0x00,
            0x00,
            0x00, // next IFD offset = 0 (none)
        )
        val exifSegment = "Exif  ".toByteArray(Charsets.US_ASCII) + tiff
        val app1Length = exifSegment.size + 2
        val app1 = byteArrayOf(
            0xFF.toByte(),
            0xE1.toByte(),
            (app1Length ushr 8).toByte(),
            (app1Length and 0xFF).toByte(),
        ) + exifSegment

        // jpegBytes[0..1] is the SOI marker (0xFFD8); splice APP1 immediately after it.
        return jpegBytes.copyOfRange(0, 2) + app1 + jpegBytes.copyOfRange(2, jpegBytes.size)
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
