package com.chriscartland.batterybutler.datanetwork.grpc

import com.chriscartland.batterybutler.domain.model.DispatcherProvider
import com.squareup.wire.GrpcCall
import com.squareup.wire.GrpcClient
import com.squareup.wire.GrpcClientStreamingCall
import com.squareup.wire.GrpcMethod
import com.squareup.wire.GrpcServerStreamingCall
import com.squareup.wire.GrpcStreamingCall
import com.squareup.wire.MessageSink
import com.squareup.wire.MessageSource
import io.ktor.client.HttpClient
import io.ktor.client.engine.darwin.Darwin
import io.ktor.client.plugins.HttpTimeout
import io.ktor.client.request.header
import io.ktor.client.request.post
import io.ktor.client.request.preparePost
import io.ktor.client.request.setBody
import io.ktor.client.statement.HttpResponse
import io.ktor.client.statement.bodyAsChannel
import io.ktor.client.statement.readRawBytes
import io.ktor.utils.io.readFully
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Job
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.channels.Channel
import kotlinx.coroutines.channels.ReceiveChannel
import kotlinx.coroutines.channels.SendChannel
import kotlinx.coroutines.launch
import kotlinx.coroutines.runBlocking
import okio.Buffer
import okio.IOException
import kotlin.coroutines.cancellation.CancellationException

actual class NetworkComponent {
    actual fun createGrpcClient(
        url: String,
        dispatcherProvider: DispatcherProvider,
    ): GrpcClient = IosGrpcClient(url, dispatcherProvider)
}

private class IosGrpcClient(
    private val url: String,
    private val dispatcherProvider: DispatcherProvider,
) : GrpcClient() {
    private val client = HttpClient(Darwin) {
        install(HttpTimeout) {
            requestTimeoutMillis = 30_000
            connectTimeoutMillis = 10_000
        }
    }

    override fun <S : Any, R : Any> newCall(method: GrpcMethod<S, R>): GrpcCall<S, R> = IosGrpcCall(client, url, method, dispatcherProvider)

    override fun <S : Any, R : Any> newStreamingCall(method: GrpcMethod<S, R>): GrpcStreamingCall<S, R> = IosGrpcStreamingCall(client, url, method, dispatcherProvider)

    // Wire 5.5 added dedicated client-/server-streaming call types to the abstract GrpcClient.
    // Wire's codegen still routes this project's only streaming RPC (SyncService/Subscribe)
    // through newStreamingCall (verified by regenerating GrpcSyncServiceClient under 5.5.1),
    // so these are never invoked. Same boundary precedent as executeBlocking below.
    override fun <S : Any, R : Any> newClientStreamingCall(method: GrpcMethod<S, R>): GrpcClientStreamingCall<S, R> = throw UnsupportedOperationException("Client-streaming calls are not supported by the iOS gRPC transport")

    override fun <S : Any, R : Any> newServerStreamingCall(method: GrpcMethod<S, R>): GrpcServerStreamingCall<S, R> = throw UnsupportedOperationException("Server-streaming calls are not supported by the iOS gRPC transport")
}

private fun frameGrpcMessage(payload: ByteArray): ByteArray {
    val buffer = Buffer()
    buffer.writeByte(0) // Compressed: 0
    buffer.writeInt(payload.size) // Length: 4 bytes
    buffer.write(payload)
    return buffer.readByteArray()
}

class IosGrpcCall<S : Any, R : Any>(
    private val client: HttpClient,
    private val baseUrl: String,
    override val method: GrpcMethod<S, R>,
    private val dispatcherProvider: DispatcherProvider,
) : GrpcCall<S, R> {
    override var requestMetadata: Map<String, String> = emptyMap()
    override val responseMetadata: Map<String, String>? = null
    override val timeout: okio.Timeout = okio.Timeout.NONE

    private var job: Job? = null
    private var cancelled = false
    private var executed = false

    override suspend fun execute(request: S): R {
        executed = true
        val path = method.path
        val fullUrl = "$baseUrl/$path"

        val requestBytes = method.requestAdapter.encode(request)
        val framedBytes = frameGrpcMessage(requestBytes)

        val response = client.post(fullUrl) {
            header("Content-Type", "application/grpc")
            header("te", "trailers")
            requestMetadata.forEach { (k, v) -> header(k, v) }
            setBody(framedBytes)
        }

        if (response.status.value != 200) {
            throw IOException("gRPC request failed with status: ${response.status}")
        }

        val bytes = response.readRawBytes()
        val buffer = Buffer().write(bytes)

        if (bytes.size >= 5) {
            buffer.skip(5)
        }

        return method.responseAdapter.decode(buffer)
    }

    override fun enqueue(
        request: S,
        callback: GrpcCall.Callback<S, R>,
    ) {
        executed = true
        // Use SupervisorJob to prevent child failures from cancelling the scope
        val scope = CoroutineScope(SupervisorJob() + dispatcherProvider.default)
        job = scope.launch {
            try {
                val result = execute(request)
                callback.onSuccess(this@IosGrpcCall, result)
            } catch (t: Throwable) {
                if (t is CancellationException) throw t
                val ioException = if (t is IOException) t else IOException(t.message)
                callback.onFailure(this@IosGrpcCall, ioException)
            }
        }
    }

    override fun executeBlocking(request: S): R =
        runBlocking {
            execute(request)
        }

    override fun isCanceled(): Boolean = cancelled

    override fun isExecuted(): Boolean = executed

    override fun clone(): GrpcCall<S, R> = IosGrpcCall(client, baseUrl, method, dispatcherProvider)

    override fun cancel() {
        cancelled = true
        job?.cancel()
    }
}

class IosGrpcStreamingCall<S : Any, R : Any>(
    private val client: HttpClient,
    private val baseUrl: String,
    override val method: GrpcMethod<S, R>,
    private val dispatcherProvider: DispatcherProvider,
) : GrpcStreamingCall<S, R> {
    override var requestMetadata: Map<String, String> = emptyMap()
    override val responseMetadata: Map<String, String>? = null
    override val timeout: okio.Timeout = okio.Timeout.NONE

    private var job: Job? = null
    private var cancelled = false
    private var executed = false

    /**
     * Executes the streaming call with an internally managed scope.
     * Note: Prefer [executeIn] with a caller-provided scope for proper lifecycle management.
     */
    @Suppress("OVERRIDE_DEPRECATION") // Wire deprecated this no-arg overload; keep override to satisfy abstract member.
    override fun execute(): Pair<SendChannel<S>, ReceiveChannel<R>> {
        // Use SupervisorJob to manage the internal scope lifecycle
        val scope = CoroutineScope(SupervisorJob() + dispatcherProvider.default)
        return executeIn(scope)
    }

    override fun executeIn(scope: CoroutineScope): Pair<SendChannel<S>, ReceiveChannel<R>> {
        executed = true
        val sendChannel = Channel<S>(Channel.UNLIMITED)
        val receiveChannel = Channel<R>(Channel.UNLIMITED)

        job = scope.launch {
            try {
                // Wait for the first message (the subscription/request)
                val request = sendChannel.receive()

                val path = method.path
                val fullUrl = "$baseUrl/$path"
                val requestBytes = method.requestAdapter.encode(request)
                val framedBytes = frameGrpcMessage(requestBytes)

                val statement = client.preparePost(fullUrl) {
                    header("Content-Type", "application/grpc")
                    header("te", "trailers")
                    requestMetadata.forEach { (k, v) -> header(k, v) }
                    setBody(framedBytes)
                }

                statement.execute { response: HttpResponse ->
                    if (response.status.value != 200) {
                        throw IOException("gRPC stream failed: ${response.status}")
                    }

                    val channel = response.bodyAsChannel()
                    while (!channel.isClosedForRead) {
                        try {
                            // Read 5-byte header
                            val headerBytes = ByteArray(5)
                            // readFully suspends until all bytes read or EOF
                            try {
                                channel.readFully(headerBytes)
                            } catch (_e: Exception) {
                                if (_e is CancellationException) throw _e
                                // If EOF happens at start of message, it's normal closure
                                break
                            }

                            val headerBuffer = Buffer().write(headerBytes)
                            headerBuffer.readByte() // Skip compression
                            val len = headerBuffer.readInt()

                            if (len > 0) {
                                val msgBytes = ByteArray(len)
                                channel.readFully(msgBytes)
                                val msg = method.responseAdapter.decode(Buffer().write(msgBytes))
                                receiveChannel.send(msg)
                            } else {
                                val emptyBuffer = Buffer()
                                val msg = method.responseAdapter.decode(emptyBuffer)
                                receiveChannel.send(msg)
                            }
                        } catch (_: kotlinx.io.IOException) {
                            break
                        } catch (e: Exception) {
                            if (e is CancellationException) throw e
                            if (channel.isClosedForRead) break
                            throw e
                        }
                    }
                }
            } catch (t: Throwable) {
                if (t is CancellationException) throw t
                receiveChannel.close(t)
            } finally {
                receiveChannel.close()
            }
        }

        return sendChannel to receiveChannel
    }

    override fun executeBlocking(): Pair<MessageSink<S>, MessageSource<R>> = throw UnsupportedOperationException("Blocking streaming not supported")

    override fun isCanceled(): Boolean = cancelled

    override fun isExecuted(): Boolean = executed

    override fun clone(): GrpcStreamingCall<S, R> = IosGrpcStreamingCall(client, baseUrl, method, dispatcherProvider)

    override fun cancel() {
        cancelled = true
        job?.cancel()
    }
}
