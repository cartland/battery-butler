package com.chriscartland.batterybutler.datanetwork

import com.chriscartland.batterybutler.domain.model.DataMode
import com.chriscartland.batterybutler.domain.model.LabsFirebaseApiKey
import kotlin.test.Test
import kotlin.test.assertEquals

class ApiKeyForModeTest {
    private val keys = LabsFirebaseApiKey(staging = "staging-key", prod = "prod-key")

    @Test
    fun labsProdModeSelectsTheProdKey() {
        assertEquals("prod-key", apiKeyForMode(DataMode.LabsProd("https://cartland-labs.web.app"), keys))
    }

    @Test
    fun labsStagingModeSelectsTheStagingKey() {
        assertEquals("staging-key", apiKeyForMode(DataMode.LabsStaging("https://cartland-labs-staging.web.app"), keys))
    }

    @Test
    fun nonLabsModesFallBackToTheStagingKey() {
        assertEquals("staging-key", apiKeyForMode(DataMode.None, keys))
        assertEquals("staging-key", apiKeyForMode(DataMode.Mock, keys))
        assertEquals("staging-key", apiKeyForMode(DataMode.GrpcAws("https://grpc"), keys))
    }
}
