package com.chriscartland.batterybutler.domain.model

import kotlin.test.Test
import kotlin.test.assertEquals

class NetworkModeTest {
    @Test
    fun testMockMode() {
        val mode = NetworkMode.Mock
        assertEquals(NetworkMode.Mock, mode)
    }
}