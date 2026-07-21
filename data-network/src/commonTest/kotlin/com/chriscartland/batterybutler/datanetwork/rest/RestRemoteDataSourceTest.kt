package com.chriscartland.batterybutler.datanetwork.rest

import com.chriscartland.batterybutler.datanetwork.LabsSyncTokenSource
import com.chriscartland.batterybutler.datanetwork.LabsTokenResult
import com.chriscartland.batterybutler.datanetwork.RemoteDataSourceState
import com.chriscartland.batterybutler.datanetwork.RemoteSyncException
import com.chriscartland.batterybutler.domain.model.Device
import com.chriscartland.batterybutler.domain.model.SyncAuthReason
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
import kotlin.test.assertFailsWith
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

            val updates = RestRemoteDataSource(client, BASE_URL, tokenSource = fakeTokens("tok123")).subscribe().toList()

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
            val ok = RestRemoteDataSource(client, BASE_URL, tokenSource = fakeTokens("tok123")).push(update)

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

    /**
     * The wire-honesty defect this PR fixes: a 401 JSON error body deserialized cleanly into
     * [SyncSnapshotWire] (all fields defaulted) and was emitted as an *empty full snapshot*,
     * so a signed-out client showed "synced". A 401 must surface as a typed auth failure and
     * must never emit an update.
     */
    @Test
    fun `subscribe surfaces a 401 JSON error body as AuthRequired instead of an empty snapshot`() =
        runTest {
            val emitted = mutableListOf<RemoteUpdate>()
            val client = mockClient { respondJson(unauthorizedBody(reason = "expired"), HttpStatusCode.Unauthorized) }

            val failure = assertFailsWith<RemoteSyncException.AuthRequired> {
                RestRemoteDataSource(client, BASE_URL, tokenSource = fakeTokens("stale"))
                    .subscribe()
                    .collect { emitted += it }
            }

            assertEquals(SyncAuthReason.TOKEN_EXPIRED, failure.reason)
            assertTrue(emitted.isEmpty(), "a 401 must not emit any update, got $emitted")
        }

    @Test
    fun `subscribe maps a 401 invalid reason to TOKEN_INVALID`() =
        runTest {
            val client = mockClient { respondJson(unauthorizedBody(reason = "invalid"), HttpStatusCode.Unauthorized) }
            val failure = assertFailsWith<RemoteSyncException.AuthRequired> {
                RestRemoteDataSource(client, BASE_URL, tokenSource = fakeTokens("bad")).subscribe().toList()
            }
            assertEquals(SyncAuthReason.TOKEN_INVALID, failure.reason)
        }

    @Test
    fun `subscribe maps a 401 without details to UNKNOWN`() =
        runTest {
            val client = mockClient {
                respondJson("""{"error":{"code":"unauthorized","message":"Sign in required"}}""", HttpStatusCode.Unauthorized)
            }
            val failure = assertFailsWith<RemoteSyncException.AuthRequired> {
                RestRemoteDataSource(client, BASE_URL, tokenSource = fakeTokens("t")).subscribe().toList()
            }
            assertEquals(SyncAuthReason.UNKNOWN, failure.reason)
        }

    @Test
    fun `subscribe maps a non-JSON 401 body to UNKNOWN`() =
        runTest {
            val client = mockClient {
                respond(
                    content = "<html>Unauthorized</html>",
                    status = HttpStatusCode.Unauthorized,
                    headers = headersOf(HttpHeaders.ContentType, "text/html"),
                )
            }
            val failure = assertFailsWith<RemoteSyncException.AuthRequired> {
                RestRemoteDataSource(client, BASE_URL, tokenSource = fakeTokens("t")).subscribe().toList()
            }
            assertEquals(SyncAuthReason.UNKNOWN, failure.reason)
        }

    @Test
    fun `push surfaces a 401 as AuthRequired with the parsed reason`() =
        runTest {
            val client = mockClient { respondJson(unauthorizedBody(reason = "expired"), HttpStatusCode.Unauthorized) }
            val failure = assertFailsWith<RemoteSyncException.AuthRequired> {
                RestRemoteDataSource(client, BASE_URL, tokenSource = fakeTokens("stale")).push(emptyUpdate())
            }
            assertEquals(SyncAuthReason.TOKEN_EXPIRED, failure.reason)
        }

    @Test
    fun `subscribe surfaces a non-auth error status as ServerError with the code retained`() =
        runTest {
            val client = mockClient { respond("boom", HttpStatusCode.InternalServerError) }
            val failure = assertFailsWith<RemoteSyncException.ServerError> {
                RestRemoteDataSource(client, BASE_URL, tokenSource = fakeTokens("t")).subscribe().toList()
            }
            assertEquals(500, failure.statusCode)
        }

    @Test
    fun `push surfaces a non-auth error status as ServerError with the code retained`() =
        runTest {
            val client = mockClient { respond("boom", HttpStatusCode.InternalServerError) }
            val failure = assertFailsWith<RemoteSyncException.ServerError> {
                RestRemoteDataSource(client, BASE_URL, tokenSource = fakeTokens("t")).push(emptyUpdate())
            }
            assertEquals(500, failure.statusCode)
        }

    @Test
    fun `subscribe with no session fires no request and surfaces AuthRequired NO_SESSION`() =
        runTest {
            var requestCount = 0
            val client = mockClient {
                requestCount++
                respondJson(SNAPSHOT_JSON)
            }

            val failure = assertFailsWith<RemoteSyncException.AuthRequired> {
                RestRemoteDataSource(client, BASE_URL, tokenSource = noSession()).subscribe().toList()
            }

            assertEquals(SyncAuthReason.NO_SESSION, failure.reason)
            assertEquals(0, requestCount, "no unauthenticated request may be fired without a session")
        }

    @Test
    fun `push with no session fires no request and surfaces AuthRequired NO_SESSION`() =
        runTest {
            var requestCount = 0
            val client = mockClient {
                requestCount++
                respondJson("""{"success":true,"message":"ok"}""")
            }

            val failure = assertFailsWith<RemoteSyncException.AuthRequired> {
                RestRemoteDataSource(client, BASE_URL, tokenSource = noSession()).push(emptyUpdate())
            }

            assertEquals(SyncAuthReason.NO_SESSION, failure.reason)
            assertEquals(0, requestCount, "no unauthenticated request may be fired without a session")
        }

    @Test
    fun `state is InvalidConfiguration for a blank baseUrl and Subscribed otherwise`() {
        val client = mockClient { respondJson("{}") }
        assertEquals(
            RemoteDataSourceState.InvalidConfiguration,
            RestRemoteDataSource(client, baseUrl = "", tokenSource = noSession()).state.value,
        )
        assertEquals(
            RemoteDataSourceState.Subscribed,
            RestRemoteDataSource(client, baseUrl = BASE_URL, tokenSource = noSession()).state.value,
        )
    }

    // region Retry-once policy (single forced refresh, then reactive session loss)

    /**
     * The stale-token happy path: a 401 with reason `expired` earns exactly one forced token
     * refresh and one retry, which succeeds — the user never sees an auth error. Against the
     * pre-fix code (no retry) this test fails: the first 401 throws AuthRequired immediately.
     */
    @Test
    fun `a 401 expired earns one forced refresh and the retried request succeeds`() =
        runTest {
            var requestCount = 0
            val seenTokens = mutableListOf<String>()
            val client = mockClient { request ->
                requestCount++
                seenTokens += request.headers[HttpHeaders.Authorization].orEmpty()
                if (requestCount == 1) {
                    respondJson(unauthorizedBody(reason = "expired"), HttpStatusCode.Unauthorized)
                } else {
                    respondJson(SNAPSHOT_JSON)
                }
            }
            val source = FakeTokenSource(
                listOf(
                    LabsTokenResult.Token("stale", servedFromCache = true),
                    LabsTokenResult.Token("fresh", servedFromCache = false),
                ),
            )

            val updates = RestRemoteDataSource(client, BASE_URL, tokenSource = source).subscribe().toList()

            assertEquals(2, requestCount, "exactly one retry")
            assertEquals(listOf("Bearer stale", "Bearer fresh"), seenTokens)
            assertEquals(1, source.forceRefreshCalls, "the retry must force a refresh")
            assertTrue(source.rejectedReasons.isEmpty(), "a recovered request must not report session loss")
            assertEquals(1, updates.size)
        }

    /** A still-401 retry is terminal: reported to the auth layer, then thrown — never a second retry. */
    @Test
    fun `a retry that still 401s reports the session rejected and throws AuthRequired`() =
        runTest {
            var requestCount = 0
            val client = mockClient {
                requestCount++
                respondJson(unauthorizedBody(reason = "expired"), HttpStatusCode.Unauthorized)
            }
            val source = FakeTokenSource(
                listOf(
                    LabsTokenResult.Token("stale", servedFromCache = true),
                    LabsTokenResult.Token("fresh", servedFromCache = false),
                ),
            )

            val failure = assertFailsWith<RemoteSyncException.AuthRequired> {
                RestRemoteDataSource(client, BASE_URL, tokenSource = source).subscribe().toList()
            }

            assertEquals(2, requestCount, "exactly one retry, never more")
            assertEquals(SyncAuthReason.TOKEN_EXPIRED, failure.reason)
            assertEquals(listOf(SyncAuthReason.TOKEN_EXPIRED), source.rejectedReasons)
        }

    /**
     * An unknown-reason 401 on a token that was *just minted* (not served from cache) is
     * authoritative: another mint can't do better, so no retry — report + throw immediately.
     */
    @Test
    fun `a 401 with unknown reason on a freshly-minted token is terminal without a retry`() =
        runTest {
            var requestCount = 0
            val client = mockClient {
                requestCount++
                respondJson("""{"error":{"code":"unauthorized","message":"Sign in required"}}""", HttpStatusCode.Unauthorized)
            }
            val source = FakeTokenSource(listOf(LabsTokenResult.Token("fresh", servedFromCache = false)))

            val failure = assertFailsWith<RemoteSyncException.AuthRequired> {
                RestRemoteDataSource(client, BASE_URL, tokenSource = source).subscribe().toList()
            }

            assertEquals(1, requestCount, "a rejection of a fresh token earns no retry")
            assertEquals(SyncAuthReason.UNKNOWN, failure.reason)
            assertEquals(listOf(SyncAuthReason.UNKNOWN), source.rejectedReasons)
        }

    /** A 401 with reason `invalid` is authoritative by definition: no retry, report + throw. */
    @Test
    fun `a 401 invalid is terminal without a retry`() =
        runTest {
            var requestCount = 0
            val client = mockClient {
                requestCount++
                respondJson(unauthorizedBody(reason = "invalid"), HttpStatusCode.Unauthorized)
            }
            val source = FakeTokenSource(listOf(LabsTokenResult.Token("t", servedFromCache = true)))

            val failure = assertFailsWith<RemoteSyncException.AuthRequired> {
                RestRemoteDataSource(client, BASE_URL, tokenSource = source).subscribe().toList()
            }

            assertEquals(1, requestCount)
            assertEquals(SyncAuthReason.TOKEN_INVALID, failure.reason)
            assertEquals(listOf(SyncAuthReason.TOKEN_INVALID), source.rejectedReasons)
        }

    /**
     * A transiently-unrefreshable token is a NETWORK problem, not an auth problem: no request is
     * fired and the typed transient failure is thrown (the sync layer maps it to a network
     * status). Against the pre-fix code (nullable token provider) this scenario surfaced as
     * AuthRequired(NO_SESSION) — a flaky network showed "sign in required".
     */
    @Test
    fun `a transient token failure throws TokenUnavailable and fires no request`() =
        runTest {
            var requestCount = 0
            val client = mockClient {
                requestCount++
                respondJson(SNAPSHOT_JSON)
            }
            val source = FakeTokenSource(listOf(LabsTokenResult.TransientFailure))

            assertFailsWith<RemoteSyncException.TokenUnavailable> {
                RestRemoteDataSource(client, BASE_URL, tokenSource = source).subscribe().toList()
            }

            assertEquals(0, requestCount)
            assertTrue(source.rejectedReasons.isEmpty(), "a transient failure is not a session rejection")
        }

    /** A session already invalidated during the token fetch surfaces as AuthRequired(TOKEN_INVALID). */
    @Test
    fun `an invalidated session surfaces as AuthRequired TOKEN_INVALID without a request`() =
        runTest {
            var requestCount = 0
            val client = mockClient {
                requestCount++
                respondJson(SNAPSHOT_JSON)
            }
            val source = FakeTokenSource(listOf(LabsTokenResult.SessionInvalidated))

            val failure = assertFailsWith<RemoteSyncException.AuthRequired> {
                RestRemoteDataSource(client, BASE_URL, tokenSource = source).push(emptyUpdate())
            }

            assertEquals(SyncAuthReason.TOKEN_INVALID, failure.reason)
            assertEquals(0, requestCount)
        }

    /** The retry policy applies to push exactly as to subscribe. */
    @Test
    fun `push retries once on a 401 expired and succeeds with the fresh token`() =
        runTest {
            var requestCount = 0
            val client = mockClient {
                requestCount++
                if (requestCount == 1) {
                    respondJson(unauthorizedBody(reason = "expired"), HttpStatusCode.Unauthorized)
                } else {
                    respondJson("""{"success":true,"message":"ok"}""")
                }
            }
            val source = FakeTokenSource(
                listOf(
                    LabsTokenResult.Token("stale", servedFromCache = true),
                    LabsTokenResult.Token("fresh", servedFromCache = false),
                ),
            )

            val ok = RestRemoteDataSource(client, BASE_URL, tokenSource = source).push(emptyUpdate())

            assertTrue(ok)
            assertEquals(2, requestCount)
            assertEquals(1, source.forceRefreshCalls)
        }

    // endregion

    /**
     * Scriptable [LabsSyncTokenSource]: serves [results] in order (the last repeats), records
     * force-refresh requests and terminal-rejection reports.
     */
    private class FakeTokenSource(
        private val results: List<LabsTokenResult>,
    ) : LabsSyncTokenSource {
        var tokenCalls = 0
        var forceRefreshCalls = 0
        val rejectedReasons = mutableListOf<SyncAuthReason>()

        override suspend fun getLabsToken(forceRefresh: Boolean): LabsTokenResult {
            if (forceRefresh) forceRefreshCalls++
            val result = results[minOf(tokenCalls, results.lastIndex)]
            tokenCalls++
            return result
        }

        override suspend fun reportSessionRejected(reason: SyncAuthReason) {
            rejectedReasons += reason
        }
    }

    private companion object {
        const val BASE_URL = "https://host.example"

        /** A source that serves each token string in order (cache-served), the last repeating. */
        fun fakeTokens(vararg tokens: String): FakeTokenSource = FakeTokenSource(tokens.map { LabsTokenResult.Token(it, servedFromCache = true) })

        fun noSession(): FakeTokenSource = FakeTokenSource(listOf(LabsTokenResult.NoSession))

        fun mockClient(handler: suspend MockRequestHandleScope.(HttpRequestData) -> HttpResponseData): HttpClient =
            HttpClient(MockEngine(handler)) {
                install(ContentNegotiation) { json(syncJson) }
            }

        fun MockRequestHandleScope.respondJson(
            content: String,
            status: HttpStatusCode = HttpStatusCode.OK,
        ): HttpResponseData =
            respond(
                content = content,
                status = status,
                headers = headersOf(HttpHeaders.ContentType, "application/json"),
            )

        /** The Labs backend's 401 envelope; `details.reason` is mid-rollout so it may be absent. */
        fun unauthorizedBody(reason: String): String = """{"error":{"code":"unauthorized","message":"Sign in required","details":{"reason":"$reason"}}}"""

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
