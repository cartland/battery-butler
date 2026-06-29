package com.chriscartland.batterybutler.datanetwork.rest

import io.ktor.client.engine.HttpClientEngine
import io.ktor.client.engine.okhttp.OkHttp

internal actual fun defaultSyncHttpClientEngine(): HttpClientEngine = OkHttp.create()
