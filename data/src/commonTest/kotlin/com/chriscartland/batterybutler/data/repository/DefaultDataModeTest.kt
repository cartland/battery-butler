package com.chriscartland.batterybutler.data.repository

import com.chriscartland.batterybutler.domain.model.DataMode
import kotlin.test.Test
import kotlin.test.assertEquals

class DefaultDataModeTest {
    @Test
    fun prodConfiguredDefaultsToProd() {
        assertEquals(
            DataMode.LabsProd("https://cartland-labs.web.app"),
            defaultDataMode(prodUrl = "https://cartland-labs.web.app", stagingUrl = ""),
        )
    }

    @Test
    fun bothConfiguredPrefersProd() {
        assertEquals(
            DataMode.LabsProd("https://cartland-labs.web.app"),
            defaultDataMode(
                prodUrl = "https://cartland-labs.web.app",
                stagingUrl = "https://cartland-labs-staging.web.app",
            ),
        )
    }

    @Test
    fun onlyStagingConfiguredDefaultsToStaging() {
        assertEquals(
            DataMode.LabsStaging("https://cartland-labs-staging.web.app"),
            defaultDataMode(prodUrl = "", stagingUrl = "https://cartland-labs-staging.web.app"),
        )
    }

    @Test
    fun neitherConfiguredDefaultsToNone() {
        assertEquals(DataMode.None, defaultDataMode(prodUrl = "", stagingUrl = ""))
        assertEquals(DataMode.None, defaultDataMode(prodUrl = "   ", stagingUrl = "   "))
    }
}
