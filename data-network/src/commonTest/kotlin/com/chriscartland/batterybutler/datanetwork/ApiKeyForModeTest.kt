package com.chriscartland.batterybutler.datanetwork

import com.chriscartland.batterybutler.domain.model.LabsFirebaseApiKey
import com.chriscartland.batterybutler.domain.model.NetworkMode
import kotlin.test.Test
import kotlin.test.assertEquals

class ApiKeyForModeTest {
    private val keys = LabsFirebaseApiKey(staging = "staging-key", prod = "prod-key")

    @Test
    fun labsProdModeSelectsTheProdKey() {
        assertEquals("prod-key", apiKeyForMode(NetworkMode.LabsProd("https://cartland-labs.web.app"), keys))
    }

    @Test
    fun labsStagingModeSelectsTheStagingKey() {
        assertEquals("staging-key", apiKeyForMode(NetworkMode.LabsStaging("https://cartland-labs-staging.web.app"), keys))
    }

    @Test
    fun nonLabsModesFallBackToTheStagingKey() {
        assertEquals("staging-key", apiKeyForMode(NetworkMode.None, keys))
        assertEquals("staging-key", apiKeyForMode(NetworkMode.Mock, keys))
        assertEquals("staging-key", apiKeyForMode(NetworkMode.GrpcAws("https://grpc"), keys))
    }
}
