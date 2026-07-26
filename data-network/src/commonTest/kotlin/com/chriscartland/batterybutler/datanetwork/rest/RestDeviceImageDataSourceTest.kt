package com.chriscartland.batterybutler.datanetwork.rest

import com.chriscartland.batterybutler.domain.model.DeviceImageError
import com.chriscartland.batterybutler.domain.model.DispatcherProvider
import com.chriscartland.batterybutler.domain.model.Result
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
import io.ktor.http.headersOf
import io.ktor.serialization.kotlinx.json.json
import io.ktor.utils.io.core.toByteArray
import kotlinx.coroutines.CoroutineDispatcher
import kotlinx.coroutines.Runnable
import kotlinx.coroutines.test.runTest
import kotlin.coroutines.CoroutineContext
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertIs
import kotlin.test.assertNull
import kotlin.test.assertTrue

class RestDeviceImageDataSourceTest {
    @Test
    fun `upload PUTs the bytes with the given content type and returns the new etag`() =
        runTest {
            var seenMethod: HttpMethod? = null
            var seenPath: String? = null
            var seenContentType: String? = null
            var seenBody: ByteArray? = null
            var seenAuth: String? = null
            val source = dataSource { request ->
                seenMethod = request.method
                seenPath = request.url.encodedPath
                seenContentType = request.body.contentType?.toString()
                seenAuth = request.headers[HttpHeaders.Authorization]
                seenBody = (request.body as io.ktor.http.content.OutgoingContent.ByteArrayContent).bytes()
                respondJson("""{"imageEtag":"etag-1"}""")
            }

            val result = source.upload("dev1", byteArrayOf(1, 2, 3), "image/jpeg")

            assertIs<Result.Success<String>>(result)
            assertEquals("etag-1", result.data)
            assertEquals(HttpMethod.Put, seenMethod)
            assertEquals("/v1/battery-butler/devices/dev1/image", seenPath)
            assertEquals("image/jpeg", seenContentType)
            assertEquals("Bearer tok123", seenAuth)
            assertEquals(listOf<Byte>(1, 2, 3), seenBody?.toList())
        }

    @Test
    fun `upload maps HTTP 400 to InvalidImage`() =
        runTest {
            val source = dataSource { respondStatus(HttpStatusCode.BadRequest) }
            val result = source.upload("dev1", byteArrayOf(), "image/jpeg")
            assertIs<Result.Error<DeviceImageError>>(result)
            assertIs<DeviceImageError.InvalidImage>(result.error)
        }

    @Test
    fun `upload maps HTTP 404 to DeviceNotFound`() =
        runTest {
            val source = dataSource { respondStatus(HttpStatusCode.NotFound) }
            val result = source.upload("dev1", byteArrayOf(), "image/jpeg")
            assertIs<Result.Error<DeviceImageError>>(result)
            assertIs<DeviceImageError.DeviceNotFound>(result.error)
        }

    @Test
    fun `upload maps HTTP 413 to TooLarge`() =
        runTest {
            val source = dataSource { respondStatus(HttpStatusCode.PayloadTooLarge) }
            val result = source.upload("dev1", byteArrayOf(), "image/jpeg")
            assertIs<Result.Error<DeviceImageError>>(result)
            assertIs<DeviceImageError.TooLarge>(result.error)
        }

    @Test
    fun `upload maps a network exception to NetworkError`() =
        runTest {
            val source = dataSource { throw RuntimeException("boom") }
            val result = source.upload("dev1", byteArrayOf(), "image/jpeg")
            assertIs<Result.Error<DeviceImageError>>(result)
            assertIs<DeviceImageError.NetworkError>(result.error)
        }

    @Test
    fun `fetch GETs the bytes and content type`() =
        runTest {
            var seenPath: String? = null
            val source = dataSource { request ->
                seenPath = request.url.encodedPath
                respond(
                    content = byteArrayOf(9, 8, 7),
                    status = HttpStatusCode.OK,
                    headers = headersOf(HttpHeaders.ContentType, "image/png"),
                )
            }

            val result = source.fetch("dev1")

            assertEquals("/v1/battery-butler/devices/dev1/image", seenPath)
            assertEquals(listOf<Byte>(9, 8, 7), result?.bytes?.toList())
            assertEquals("image/png", result?.contentType)
        }

    @Test
    fun `fetch returns null on HTTP 404`() =
        runTest {
            val source = dataSource { respondStatus(HttpStatusCode.NotFound) }
            assertNull(source.fetch("dev1"))
        }

    @Test
    fun `fetch returns null on a network exception`() =
        runTest {
            val source = dataSource { throw RuntimeException("boom") }
            assertNull(source.fetch("dev1"))
        }

    @Test
    fun `delete DELETEs and returns true on success`() =
        runTest {
            var seenMethod: HttpMethod? = null
            val source = dataSource { request ->
                seenMethod = request.method
                respondJson("""{"success":true,"message":""}""")
            }
            assertTrue(source.delete("dev1"))
            assertEquals(HttpMethod.Delete, seenMethod)
        }

    @Test
    fun `delete is idempotent -- true even when there was no image`() =
        runTest {
            val source = dataSource { respondJson("""{"success":true,"message":""}""") }
            assertTrue(source.delete("dev1"))
        }

    @Test
    fun `delete returns false on a network exception`() =
        runTest {
            val source = dataSource { throw RuntimeException("boom") }
            assertEquals(false, source.delete("dev1"))
        }

    @Test
    fun `network calls run on the injected IO dispatcher`() =
        runTest {
            val recordingIo = RecordingDispatcher()
            val client = HttpClient(MockEngine { respondStatus(HttpStatusCode.NotFound) }) {
                install(ContentNegotiation) { json(syncJson) }
            }
            val source = RestDeviceImageDataSource(
                httpClient = client,
                baseUrl = "https://labs.example.com",
                tokenProvider = { "tok123" },
                dispatcherProvider = TestDispatchers(recordingIo),
            )

            source.fetch("dev1")

            assertTrue(recordingIo.dispatches > 0, "fetch() must dispatch onto the injected IO dispatcher")
        }

    /** Immediate dispatcher that records whether it was dispatched to. */
    private class RecordingDispatcher : CoroutineDispatcher() {
        var dispatches = 0

        override fun dispatch(
            context: CoroutineContext,
            block: Runnable,
        ) {
            dispatches++
            block.run()
        }
    }

    private class TestDispatchers(
        private val dispatcher: CoroutineDispatcher,
    ) : DispatcherProvider {
        override val default: CoroutineDispatcher get() = dispatcher
        override val io: CoroutineDispatcher get() = dispatcher
        override val main: CoroutineDispatcher get() = dispatcher
    }

    private companion object {
        fun dataSource(handler: suspend MockRequestHandleScope.(HttpRequestData) -> HttpResponseData): RestDeviceImageDataSource {
            val client = HttpClient(MockEngine(handler)) {
                install(ContentNegotiation) { json(syncJson) }
            }
            return RestDeviceImageDataSource(httpClient = client, baseUrl = "https://labs.example.com", tokenProvider = { "tok123" })
        }

        fun MockRequestHandleScope.respondJson(content: String): HttpResponseData =
            respond(
                content = content,
                status = HttpStatusCode.OK,
                headers = headersOf(HttpHeaders.ContentType, "application/json"),
            )

        fun MockRequestHandleScope.respondStatus(status: HttpStatusCode): HttpResponseData = respond(content = "".toByteArray(), status = status)
    }
}
