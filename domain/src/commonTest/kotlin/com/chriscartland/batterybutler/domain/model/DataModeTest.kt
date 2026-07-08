package com.chriscartland.batterybutler.domain.model

import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertIs
import kotlin.test.assertNotEquals
import kotlin.test.assertNull
import kotlin.test.assertTrue

class DataModeTest {
    @Test
    fun `Mock is singleton instance`() {
        val mock1 = DataMode.Mock
        val mock2 = DataMode.Mock

        assertEquals(mock1, mock2)
    }

    @Test
    fun `GrpcLocal stores url`() {
        val mode = DataMode.GrpcLocal(url = "http://localhost:8080")

        assertEquals("http://localhost:8080", mode.url)
    }

    @Test
    fun `GrpcLocal can have null url`() {
        val mode = DataMode.GrpcLocal(url = null)

        assertNull(mode.url)
    }

    @Test
    fun `GrpcLocal instances with same url are equal`() {
        val mode1 = DataMode.GrpcLocal(url = "http://localhost:8080")
        val mode2 = DataMode.GrpcLocal(url = "http://localhost:8080")

        assertEquals(mode1, mode2)
        assertEquals(mode1.hashCode(), mode2.hashCode())
    }

    @Test
    fun `GrpcLocal instances with different urls are not equal`() {
        val mode1 = DataMode.GrpcLocal(url = "http://localhost:8080")
        val mode2 = DataMode.GrpcLocal(url = "http://localhost:9090")

        assertNotEquals(mode1, mode2)
    }

    @Test
    fun `GrpcAws stores url`() {
        val mode = DataMode.GrpcAws(url = "https://api.example.com")

        assertEquals("https://api.example.com", mode.url)
    }

    @Test
    fun `GrpcAws can have null url`() {
        val mode = DataMode.GrpcAws(url = null)

        assertNull(mode.url)
    }

    @Test
    fun `GrpcAws instances with same url are equal`() {
        val mode1 = DataMode.GrpcAws(url = "https://api.example.com")
        val mode2 = DataMode.GrpcAws(url = "https://api.example.com")

        assertEquals(mode1, mode2)
        assertEquals(mode1.hashCode(), mode2.hashCode())
    }

    @Test
    fun `GrpcDev stores url`() {
        val mode = DataMode.GrpcDev(url = "http://dev.example.com")

        assertEquals("http://dev.example.com", mode.url)
    }

    @Test
    fun `GrpcDev can have null url`() {
        val mode = DataMode.GrpcDev(url = null)

        assertNull(mode.url)
    }

    @Test
    fun `GrpcDev instances with same url are equal`() {
        val mode1 = DataMode.GrpcDev(url = "http://dev.example.com")
        val mode2 = DataMode.GrpcDev(url = "http://dev.example.com")

        assertEquals(mode1, mode2)
        assertEquals(mode1.hashCode(), mode2.hashCode())
    }

    @Test
    fun `Labs modes store url and can be null`() {
        assertEquals("https://staging.example", DataMode.LabsStaging("https://staging.example").url)
        assertEquals("https://prod.example", DataMode.LabsProd("https://prod.example").url)
        assertNull(DataMode.LabsStaging(null).url)
        assertNull(DataMode.LabsProd(null).url)
    }

    @Test
    fun `sealed interface variants are distinguishable`() {
        val mock: DataMode = DataMode.Mock
        val local: DataMode = DataMode.GrpcLocal("http://localhost")
        val aws: DataMode = DataMode.GrpcAws("https://aws.example.com")
        val dev: DataMode = DataMode.GrpcDev("http://dev.example.com")
        val labsStaging: DataMode = DataMode.LabsStaging("https://staging.example")
        val labsProd: DataMode = DataMode.LabsProd("https://prod.example")

        assertIs<DataMode.Mock>(mock)
        assertIs<DataMode.GrpcLocal>(local)
        assertIs<DataMode.GrpcAws>(aws)
        assertIs<DataMode.GrpcDev>(dev)
        assertIs<DataMode.LabsStaging>(labsStaging)
        assertIs<DataMode.LabsProd>(labsProd)
    }

    @Test
    fun `when expression covers all variants`() {
        val modes = listOf(
            DataMode.Mock,
            DataMode.None,
            DataMode.GrpcLocal("http://localhost"),
            DataMode.GrpcAws("https://aws.example.com"),
            DataMode.GrpcDev("http://dev.example.com"),
            DataMode.LabsStaging("https://staging.example"),
            DataMode.LabsProd("https://prod.example"),
        )

        for (mode in modes) {
            val description = when (mode) {
                is DataMode.Mock -> "mock"
                is DataMode.None -> "none"
                is DataMode.GrpcLocal -> "local"
                is DataMode.GrpcAws -> "aws"
                is DataMode.GrpcDev -> "dev"
                is DataMode.LabsStaging -> "labs_staging"
                is DataMode.LabsProd -> "labs_prod"
            }
            // If we get here without error, the when is exhaustive
            assertTrue(description.isNotEmpty())
        }
    }

    @Test
    fun `different variant types with same url are not equal`() {
        val local = DataMode.GrpcLocal(url = "http://example.com")
        val aws = DataMode.GrpcAws(url = "http://example.com")
        val dev = DataMode.GrpcDev(url = "http://example.com")

        assertNotEquals<DataMode>(local, aws)
        assertNotEquals<DataMode>(local, dev)
        assertNotEquals<DataMode>(aws, dev)
    }
}
