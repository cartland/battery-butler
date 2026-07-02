package com.chriscartland.batterybutler.data.repository

import com.chriscartland.batterybutler.domain.model.NetworkMode
import kotlin.test.Test
import kotlin.test.assertEquals

class DefaultNetworkModeTest {
    @Test
    fun prodConfiguredDefaultsToProd() {
        assertEquals(
            NetworkMode.LabsProd("https://cartland-labs.web.app"),
            defaultNetworkMode(prodUrl = "https://cartland-labs.web.app", stagingUrl = ""),
        )
    }

    @Test
    fun bothConfiguredPrefersProd() {
        assertEquals(
            NetworkMode.LabsProd("https://cartland-labs.web.app"),
            defaultNetworkMode(
                prodUrl = "https://cartland-labs.web.app",
                stagingUrl = "https://cartland-labs-staging.web.app",
            ),
        )
    }

    @Test
    fun onlyStagingConfiguredDefaultsToStaging() {
        assertEquals(
            NetworkMode.LabsStaging("https://cartland-labs-staging.web.app"),
            defaultNetworkMode(prodUrl = "", stagingUrl = "https://cartland-labs-staging.web.app"),
        )
    }

    @Test
    fun neitherConfiguredDefaultsToNone() {
        assertEquals(NetworkMode.None, defaultNetworkMode(prodUrl = "", stagingUrl = ""))
        assertEquals(NetworkMode.None, defaultNetworkMode(prodUrl = "   ", stagingUrl = "   "))
    }
}
