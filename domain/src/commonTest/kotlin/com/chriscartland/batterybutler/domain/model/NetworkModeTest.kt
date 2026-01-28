package com.chriscartland.batterybutler.domain.model

import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertIs
import kotlin.test.assertNotEquals
import kotlin.test.assertNull
import kotlin.test.assertTrue

class NetworkModeTest {
    @Test
    fun `Mock is singleton instance`() {
        val mock1 = NetworkMode.Mock
        val mock2 = NetworkMode.Mock

        assertEquals(mock1, mock2)
    }

    @Test
    fun `GrpcLocal stores url`() {
        val mode = NetworkMode.GrpcLocal(url = "http://localhost:8080")

        assertEquals("http://localhost:8080", mode.url)
    }

    @Test
    fun `GrpcLocal can have null url`() {
        val mode = NetworkMode.GrpcLocal(url = null)

        assertNull(mode.url)
    }

    @Test
    fun `GrpcLocal instances with same url are equal`() {
        val mode1 = NetworkMode.GrpcLocal(url = "http://localhost:8080")
        val mode2 = NetworkMode.GrpcLocal(url = "http://localhost:8080")

        assertEquals(mode1, mode2)
        assertEquals(mode1.hashCode(), mode2.hashCode())
    }

    @Test
    fun `GrpcLocal instances with different urls are not equal`() {
        val mode1 = NetworkMode.GrpcLocal(url = "http://localhost:8080")
        val mode2 = NetworkMode.GrpcLocal(url = "http://localhost:9090")

        assertNotEquals(mode1, mode2)
    }

    @Test
    fun `GrpcAws stores url`() {
        val mode = NetworkMode.GrpcAws(url = "https://api.example.com")

        assertEquals("https://api.example.com", mode.url)
    }

    @Test
    fun `GrpcAws can have null url`() {
        val mode = NetworkMode.GrpcAws(url = null)

        assertNull(mode.url)
    }

    @Test
    fun `GrpcAws instances with same url are equal`() {
        val mode1 = NetworkMode.GrpcAws(url = "https://api.example.com")
        val mode2 = NetworkMode.GrpcAws(url = "https://api.example.com")

        assertEquals(mode1, mode2)
        assertEquals(mode1.hashCode(), mode2.hashCode())
    }

    @Test
    fun `sealed interface variants are distinguishable`() {
        val mock: NetworkMode = NetworkMode.Mock
        val local: NetworkMode = NetworkMode.GrpcLocal("http://localhost")
        val aws: NetworkMode = NetworkMode.GrpcAws("https://aws.example.com")

        assertIs<NetworkMode.Mock>(mock)
        assertIs<NetworkMode.GrpcLocal>(local)
        assertIs<NetworkMode.GrpcAws>(aws)
    }

    @Test
    fun `when expression covers all variants`() {
        val modes = listOf(
            NetworkMode.Mock,
            NetworkMode.GrpcLocal("http://localhost"),
            NetworkMode.GrpcAws("https://aws.example.com"),
        )

        for (mode in modes) {
            val description = when (mode) {
                is NetworkMode.Mock -> "mock"
                is NetworkMode.GrpcLocal -> "local"
                is NetworkMode.GrpcAws -> "aws"
            }
            // If we get here without error, the when is exhaustive
            assertTrue(description.isNotEmpty())
        }
    }

    @Test
    fun `different variant types with same url are not equal`() {
        val local = NetworkMode.GrpcLocal(url = "http://example.com")
        val aws = NetworkMode.GrpcAws(url = "http://example.com")

        assertNotEquals<NetworkMode>(local, aws)
    }
}
