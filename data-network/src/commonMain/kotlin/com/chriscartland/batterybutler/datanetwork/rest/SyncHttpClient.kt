package com.chriscartland.batterybutler.datanetwork.rest

import io.ktor.client.HttpClient
import io.ktor.client.engine.HttpClientEngine
import io.ktor.client.plugins.contentnegotiation.ContentNegotiation
import io.ktor.serialization.kotlinx.json.json
import kotlinx.serialization.json.Json

/*
 * Ktor HTTP client for the Labs REST sync backend.
 *
 * One Json config serves both directions of the defensive wire boundary (see [SyncDto]):
 *   - encodeDefaults = true   -> a push is canonical (every field present)
 *   - ignoreUnknownKeys = true -> a read is lenient (an additive field from a newer server
 *                                 is ignored, not a parse failure)
 *
 * The engine is chosen per platform via [defaultSyncHttpClientEngine] (Darwin on iOS, OkHttp
 * on Android + JVM/desktop) — explicit, rather than relying on Ktor's classpath
 * auto-discovery, which is unreliable on Kotlin/Native.
 */
internal val syncJson: Json = Json {
    ignoreUnknownKeys = true
    encodeDefaults = true
}

internal expect fun defaultSyncHttpClientEngine(): HttpClientEngine

internal fun createSyncHttpClient(
    engine: HttpClientEngine = defaultSyncHttpClientEngine(),
): HttpClient =
    HttpClient(engine) {
        install(ContentNegotiation) {
            json(syncJson)
        }
    }
