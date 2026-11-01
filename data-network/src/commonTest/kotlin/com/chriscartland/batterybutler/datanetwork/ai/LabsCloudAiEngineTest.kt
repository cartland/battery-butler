package com.chriscartland.batterybutler.datanetwork.ai

import com.chriscartland.batterybutler.datanetwork.LabsAuthGateway
import com.chriscartland.batterybutler.datanetwork.LabsSessionInvalidation
import com.chriscartland.batterybutler.datanetwork.LabsSignInIdentity
import com.chriscartland.batterybutler.datanetwork.LabsTokenResult
import com.chriscartland.batterybutler.datanetwork.rest.createSyncHttpClient
import com.chriscartland.batterybutler.domain.model.AuthError
import com.chriscartland.batterybutler.domain.model.DataMode
import com.chriscartland.batterybutler.domain.model.DispatcherProvider
import com.chriscartland.batterybutler.domain.model.Result
import com.chriscartland.batterybutler.domain.model.SyncAuthReason
import com.chriscartland.batterybutler.domain.model.ai.AiMessage
import com.chriscartland.batterybutler.domain.repository.DataModeRepository
import io.ktor.client.engine.mock.MockEngine
import io.ktor.client.engine.mock.MockRequestHandleScope
import io.ktor.client.engine.mock.respond
import io.ktor.client.request.HttpRequestData
import io.ktor.client.request.HttpResponseData
import io.ktor.http.HttpHeaders
import io.ktor.http.HttpStatusCode
import io.ktor.http.content.TextContent
import io.ktor.http.headersOf
import kotlinx.coroutines.CoroutineDispatcher
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.emptyFlow
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.flow.toList
import kotlinx.coroutines.test.StandardTestDispatcher
import kotlinx.coroutines.test.TestScope
import kotlinx.coroutines.test.runTest
import kotlinx.serialization.json.Json
import kotlinx.serialization.json.JsonObject
import kotlinx.serialization.json.jsonArray
import kotlinx.serialization.json.jsonObject
import kotlinx.serialization.json.jsonPrimitive
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertFalse
import kotlin.test.assertTrue

class LabsCloudAiEngineTest {
    @Test
    fun `a plain answer posts the prompt with the tools and returns the model text`() =
        runTest {
            val requests = mutableListOf<JsonObject>()
            val engine = engine(this, requests) { respondJson(modelReply("You have 2 smoke alarms.")) }

            val messages = engine.generateResponse("How many smoke alarms?").toList()

            assertEquals(listOf("You have 2 smoke alarms."), messages.map(AiMessage::text))
            val body = requests.single()
            assertTrue(body["systemInstruction"]!!.jsonPrimitive.content.startsWith("You are Battery Butler"))
            assertEquals(9, body["tools"]!!.jsonArray.size)
            assertEquals(
                "How many smoke alarms?",
                body["contents"]!!
                    .jsonArray
                    .single()
                    .jsonObject["parts"]!!
                    .jsonArray[0]
                    .jsonObject["text"]!!
                    .jsonPrimitive.content,
            )
        }

    @Test
    fun `a tool call runs the handler and echoes the model turn verbatim with its signature`() =
        runTest {
            val requests = mutableListOf<JsonObject>()
            val callTurn =
                """{"role":"model","parts":[{"functionCall":{"id":"c1","name":"addDevice","args":{"name":"Hall Alarm"}},"thoughtSignature":"c2ln"}]}"""
            var n = 0
            val engine = engine(this, requests) {
                if (n++ == 0) respondJson("""{"content":$callTurn,"modelVersion":"m"}""") else respondJson(modelReply("Added."))
            }
            val toolCalls = mutableListOf<Pair<String, Map<String, Any?>>>()

            val messages = engine
                .generateResponse("Add Hall Alarm") { name, args ->
                    toolCalls += name to args
                    "{\"status\":\"ok\"}"
                }.toList()

            assertEquals(listOf("Added."), messages.map(AiMessage::text))
            assertEquals(listOf("addDevice" to mapOf<String, Any?>("name" to "Hall Alarm")), toolCalls)
            val second = requests[1]["contents"]!!.jsonArray
            assertEquals(Json.parseToJsonElement(callTurn), second[1])
            val response = second[2]
                .jsonObject["parts"]!!
                .jsonArray[0]
                .jsonObject["functionResponse"]!!
                .jsonObject
            assertEquals("c1", response["id"]!!.jsonPrimitive.content)
            assertEquals("{\"status\":\"ok\"}", response["response"]!!.jsonObject["result"]!!.jsonPrimitive.content)
        }

    @Test
    fun `thought parts are not shown`() =
        runTest {
            val engine = engine(this) {
                respondJson(
                    """{"content":{"role":"model","parts":[{"text":"pondering","thought":true},{"text":"Answer"}]},"modelVersion":""}""",
                )
            }
            assertEquals("Answer", engine.generateResponse("q").first().text)
        }

    @Test
    fun `the conversation carries over to the next message`() =
        runTest {
            val requests = mutableListOf<JsonObject>()
            val engine = engine(this, requests) { respondJson(modelReply("ok")) }
            engine.generateResponse("first").toList()
            engine.generateResponse("second").toList()
            assertEquals(3, requests[1]["contents"]!!.jsonArray.size)
        }

    @Test
    fun `a failure rolls the conversation back and says why`() =
        runTest {
            val requests = mutableListOf<JsonObject>()
            var fail = true
            val engine = engine(this, requests) {
                if (fail) respond("", HttpStatusCode.BadGateway) else respondJson(modelReply("ok"))
            }

            val failed = engine.generateResponse("first").first()
            assertEquals(LabsCloudAiEngine.UNAVAILABLE_MESSAGE, failed.text)

            fail = false
            engine.generateResponse("second").toList()
            assertEquals(1, requests.last()["contents"]!!.jsonArray.size, "the failed turn was not kept")
        }

    @Test
    fun `403 says the account lacks access`() =
        runTest {
            val engine = engine(this) { respond("", HttpStatusCode.Forbidden) }
            assertEquals(LabsCloudAiEngine.FORBIDDEN_MESSAGE, engine.generateResponse("q").first().text)
        }

    @Test
    fun `no session asks the user to sign in without calling the backend`() =
        runTest {
            val requests = mutableListOf<JsonObject>()
            val engine = engine(this, requests, token = null) { respondJson(modelReply("unused")) }
            assertEquals(LabsCloudAiEngine.SIGN_IN_MESSAGE, engine.generateResponse("q").first().text)
            assertTrue(requests.isEmpty())
        }

    @Test
    fun `outside a Labs mode it is unavailable and says how to fix it`() =
        runTest {
            val engine = engine(this, mode = DataMode.Mock) { respondJson(modelReply("unused")) }
            assertFalse(engine.isAvailable.first())
            assertEquals(LabsCloudAiEngine.NOT_LABS_MESSAGE, engine.generateResponse("q").first().text)
        }

    @Test
    fun `a Labs mode with no URL is unavailable`() =
        runTest {
            val engine = engine(this, mode = DataMode.LabsProd(url = "")) { respondJson(modelReply("unused")) }
            assertFalse(engine.isAvailable.first())
        }

    @Test
    fun `trimming drops whole exchanges from the oldest end and keeps the newest message`() =
        runTest {
            val requests = mutableListOf<JsonObject>()
            val engine = engine(this, requests) { respondJson(modelReply("ok")) }
            val big = "x".repeat(LabsCloudAiEngine.MAX_REQUEST_CHARS / 3)
            repeat(4) { engine.generateResponse("$it$big").toList() }

            val contents = requests.last()["contents"]!!.jsonArray
            assertTrue(contents.sumOf { it.toString().length } <= LabsCloudAiEngine.MAX_REQUEST_CHARS)
            assertEquals(
                "user",
                contents
                    .first()
                    .jsonObject["role"]!!
                    .jsonPrimitive.content,
            )
            assertTrue(contents.last().toString().contains("3x"))
        }

    // ─── helpers ────────────────────────────────────────────────────────────────

    private fun modelReply(text: String) = """{"content":{"role":"model","parts":[{"text":"$text"}]},"modelVersion":"m"}"""

    private fun MockRequestHandleScope.respondJson(body: String): HttpResponseData = respond(body, HttpStatusCode.OK, headersOf(HttpHeaders.ContentType, "application/json"))

    private fun engine(
        scope: TestScope,
        requests: MutableList<JsonObject> = mutableListOf(),
        mode: DataMode = DataMode.LabsStaging(url = "https://staging.example"),
        token: String? = "tok",
        handler: MockRequestHandleScope.(HttpRequestData) -> HttpResponseData,
    ): LabsCloudAiEngine {
        val dispatcher = StandardTestDispatcher(scope.testScheduler)
        return LabsCloudAiEngine(
            dataModeRepository = FakeDataModeRepository(mode),
            labsAuthGateway = FakeGateway(token),
            dispatcherProvider = TestDispatchers(dispatcher),
        ).apply {
            httpClientFactory = {
                createSyncHttpClient(
                    MockEngine { request ->
                        assertEquals("/v1/battery-butler/ai/generate", request.url.encodedPath)
                        assertEquals("Bearer $token", request.headers[HttpHeaders.Authorization])
                        requests += Json.parseToJsonElement((request.body as TextContent).text).jsonObject
                        handler(request)
                    },
                )
            }
        }
    }

    private class FakeDataModeRepository(
        mode: DataMode,
    ) : DataModeRepository {
        override val dataMode = MutableStateFlow(mode)

        override suspend fun setDataMode(mode: DataMode) {
            dataMode.value = mode
        }
    }

    private class FakeGateway(
        private val token: String?,
    ) : LabsAuthGateway {
        override suspend fun getLabsIdToken(): String? = token

        override suspend fun getLabsToken(forceRefresh: Boolean): LabsTokenResult = token?.let { LabsTokenResult.Token(it, servedFromCache = true) } ?: LabsTokenResult.NoSession

        override suspend fun reportSessionRejected(reason: SyncAuthReason) = Unit

        override suspend fun signInToLabsWithGoogle(googleIdToken: String): Result<LabsSignInIdentity, AuthError> = error("unused")

        override val sessionInvalidations: Flow<LabsSessionInvalidation> = emptyFlow()

        override suspend fun signOutLabs() = Unit

        override suspend fun restoreSession(): Result<Unit, AuthError> = error("unused")
    }

    private class TestDispatchers(
        dispatcher: CoroutineDispatcher,
    ) : DispatcherProvider {
        override val default = dispatcher
        override val io = dispatcher
        override val main = dispatcher
    }
}
