package com.chriscartland.batterybutler.datanetwork.rest

import com.chriscartland.batterybutler.datanetwork.RemoteDataSourceState
import com.chriscartland.batterybutler.domain.model.Device
import com.chriscartland.batterybutler.domain.repository.RemoteUpdate
import io.ktor.client.HttpClient
import io.ktor.client.engine.mock.MockEngine
import io.ktor.client.engine.mock.MockRequestHandleScope
import io.ktor.client.engine.mock.respond
import io.ktor.client.plugins.contentnegotiation.ContentNegotiation
import io.ktor.client.request.HttpRequestData
import io.ktor.client.request.HttpResponseData
import io.ktor.http.HttpHeaders
import io.ktor.http.HttpMethod
import io.ktor.http.HttpStatusCode
import io.ktor.http.content.TextContent
import io.ktor.http.headersOf
import io.ktor.serialization.kotlinx.json.json
import kotlinx.coroutines.flow.toList
import kotlinx.coroutines.test.runTest
import kotlinx.serialization.json.jsonObject
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertFalse
import kotlin.test.assertTrue
import kotlin.time.Instant

class RestRemoteDataSourceTest {
    @Test
    fun `subscribe issues GET sync with bearer auth and emits the mapped snapshot`() =
        runTest {
            var seenMethod: HttpMethod? = null
            var seenPath: String? = null
            var seenAuth: String? = null
            val client = mockClient { request ->
                seenMethod = request.method
                seenPath = request.url.encodedPath
                seenAuth = request.headers[HttpHeaders.Authorization]
                respondJson(SNAPSHOT_JSON)
            }

            val updates = RestRemoteDataSource(client, BASE_URL, tokenProvider = { "tok123" }).subscribe().toList()

            assertEquals(HttpMethod.Get, seenMethod)
            assertEquals("/v1/battery-butler/sync", seenPath)
            assertEquals("Bearer tok123", seenAuth)
            val update = updates.single()
            assertTrue(update.isFullSnapshot)
            assertEquals("type-smoke", update.deviceTypes.single().id)
            assertEquals("Kitchen", update.devices.single().location)
            assertEquals("Duracell", update.events.single().notes)
        }

    @Test
    fun `push POSTs the canonical patch and returns success`() =
        runTest {
            var body: String? = null
            var seenMethod: HttpMethod? = null
            val client = mockClient { request ->
                seenMethod = request.method
                body = (request.body as TextContent).text
                respondJson("""{"success":true,"message":"ok"}""")
            }

            val update = RemoteUpdate(
                isFullSnapshot = false,
                deviceTypes = emptyList(),
                devices = listOf(
                    Device(
                        id = "d1",
                        name = "Kitchen Alarm",
                        typeId = "type-smoke",
                        batteryLastReplaced = Instant.fromEpochMilliseconds(0),
                        lastUpdated = Instant.fromEpochMilliseconds(0),
                        location = null,
                    ),
                ),
                events = emptyList(),
                deletedDeviceIds = listOf("d-old"),
            )
            val ok = RestRemoteDataSource(client, BASE_URL, tokenProvider = { "tok123" }).push(update)

            assertTrue(ok)
            assertEquals(HttpMethod.Post, seenMethod)
            val bodyText = requireNotNull(body) { "POST body was not captured" }
            val json = syncJson.parseToJsonElement(bodyText).jsonObject
            // Canonical envelope: every array field present (encodeDefaults).
            assertEquals(
                setOf("deviceTypes", "devices", "events", "deletedDeviceTypeIds", "deletedDeviceIds", "deletedEventIds"),
                json.keys,
            )
            // A null domain location serializes to "" on the wire (mapper convention).
            val device = syncJson.decodeFromString<SyncPushRequestWire>(bodyText).devices.single()
            assertEquals("", device.location)
            assertEquals("type-smoke", device.typeId)
        }

    @Test
    fun `no Authorization header is sent when the token is null`() =
        runTest {
            var hadAuth = true
            val client = mockClient { request ->
                hadAuth = request.headers.contains(HttpHeaders.Authorization)
                respondJson(SNAPSHOT_JSON)
            }

            RestRemoteDataSource(client, BASE_URL, tokenProvider = { null }).subscribe().toList()

            assertFalse(hadAuth)
        }

    @Test
    fun `push returns false on a server error instead of throwing`() =
        runTest {
            val client = mockClient { respond("boom", HttpStatusCode.InternalServerError) }
            val ok = RestRemoteDataSource(client, BASE_URL, tokenProvider = { "t" }).push(emptyUpdate())
            assertFalse(ok)
        }

    @Test
    fun `subscribe emits nothing on a server error instead of throwing`() =
        runTest {
            val client = mockClient { respond("boom", HttpStatusCode.InternalServerError) }
            val updates = RestRemoteDataSource(client, BASE_URL, tokenProvider = { "t" }).subscribe().toList()
            assertTrue(updates.isEmpty())
        }

    @Test
    fun `state is InvalidConfiguration for a blank baseUrl and Subscribed otherwise`() {
        val client = mockClient { respondJson("{}") }
        assertEquals(
            RemoteDataSourceState.InvalidConfiguration,
            RestRemoteDataSource(client, baseUrl = "", tokenProvider = { null }).state.value,
        )
        assertEquals(
            RemoteDataSourceState.Subscribed,
            RestRemoteDataSource(client, baseUrl = BASE_URL, tokenProvider = { null }).state.value,
        )
    }

    private companion object {
        const val BASE_URL = "https://host.example"

        fun mockClient(handler: suspend MockRequestHandleScope.(HttpRequestData) -> HttpResponseData): HttpClient =
            HttpClient(MockEngine(handler)) {
                install(ContentNegotiation) { json(syncJson) }
            }

        fun MockRequestHandleScope.respondJson(content: String): HttpResponseData =
            respond(
                content = content,
                status = HttpStatusCode.OK,
                headers = headersOf(HttpHeaders.ContentType, "application/json"),
            )

        fun emptyUpdate() =
            RemoteUpdate(
                isFullSnapshot = false,
                deviceTypes = emptyList(),
                devices = emptyList(),
                events = emptyList(),
            )

        val SNAPSHOT_JSON =
            """
            {
              "deviceTypes": [
                { "id": "type-smoke", "name": "Smoke Alarm", "defaultIcon": "detector_smoke", "batteryType": "9V", "batteryQuantity": 2 }
              ],
              "devices": [
                {
                  "id": "device-kitchen", "name": "Kitchen Alarm", "typeId": "type-smoke", "location": "Kitchen",
                  "batteryLastReplacedTimestampMs": 1704067200000, "lastUpdatedTimestampMs": 1704153600000, "imagePath": "/img/a.jpg"
                }
              ],
              "events": [
                { "id": "e1", "deviceId": "device-kitchen", "dateTimestampMs": 1704067200000, "createdTimestampMs": 1704067300000, "notes": "Duracell" }
              ]
            }
            """.trimIndent()
    }
}
