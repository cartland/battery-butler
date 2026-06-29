package com.chriscartland.batterybutler.datanetwork.rest

import io.ktor.client.engine.HttpClientEngine
import io.ktor.client.engine.darwin.Darwin

internal actual fun defaultSyncHttpClientEngine(): HttpClientEngine = Darwin.create()
