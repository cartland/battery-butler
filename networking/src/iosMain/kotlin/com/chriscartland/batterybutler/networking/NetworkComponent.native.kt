package com.chriscartland.batterybutler.networking

import com.squareup.wire.GrpcCall
import com.squareup.wire.GrpcClient
import com.squareup.wire.GrpcMethod
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
import io.ktor.client.statement.readBytes
import io.ktor.utils.io.core.readBytes
import io.ktor.utils.io.readFully
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.channels.Channel
import kotlinx.coroutines.channels.ReceiveChannel
import kotlinx.coroutines.channels.SendChannel
import kotlinx.coroutines.launch
import kotlinx.coroutines.runBlocking
import okio.Buffer
import okio.IOException

actual class NetworkComponent {
    actual val grpcClient: GrpcClient by lazy {
        IosGrpcClient()
    }
}

class IosGrpcClient : GrpcClient() {
    private val client = HttpClient(Darwin) {
        install(HttpTimeout) {
            requestTimeoutMillis = 30_000
            connectTimeoutMillis = 10_000
        }
    }

    override fun <S : Any, R : Any> newCall(method: GrpcMethod<S, R>): GrpcCall<S, R> = IosGrpcCall(client, method)

    override fun <S : Any, R : Any> newStreamingCall(method: GrpcMethod<S, R>): GrpcStreamingCall<S, R> =
        IosGrpcStreamingCall(client, method)
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
    override val method: GrpcMethod<S, R>,
) : GrpcCall<S, R> {
    override var requestMetadata: Map<String, String> = emptyMap()
    override val responseMetadata: Map<String, String>? = null
    override val timeout: okio.Timeout = okio.Timeout.NONE

    override suspend fun execute(request: S): R {
        val path = method.path
        val fullUrl = "http://localhost:50051/$path"

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

        val bytes = response.readBytes()
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
        val scope = CoroutineScope(kotlinx.coroutines.Dispatchers.Default)
        scope.launch {
            try {
                val result = execute(request)
                callback.onSuccess(this@IosGrpcCall, result)
            } catch (t: Throwable) {
                val ioException = if (t is IOException) t else IOException(t.message)
                callback.onFailure(this@IosGrpcCall, ioException)
            }
        }
    }

    override fun executeBlocking(request: S): R =
        runBlocking {
            execute(request)
        }

    override fun isCanceled(): Boolean = false

    override fun isExecuted(): Boolean = false

    override fun clone(): GrpcCall<S, R> = IosGrpcCall(client, method)

    override fun cancel() {}
}

class IosGrpcStreamingCall<S : Any, R : Any>(
    private val client: HttpClient,
    override val method: GrpcMethod<S, R>,
) : GrpcStreamingCall<S, R> {
    override var requestMetadata: Map<String, String> = emptyMap()
    override val responseMetadata: Map<String, String>? = null
    override val timeout: okio.Timeout = okio.Timeout.NONE

    override fun execute(): Pair<SendChannel<S>, ReceiveChannel<R>> {
        val scope = CoroutineScope(kotlinx.coroutines.Dispatchers.Default)
        return executeIn(scope)
    }

    override fun executeIn(scope: CoroutineScope): Pair<SendChannel<S>, ReceiveChannel<R>> {
        val sendChannel = Channel<S>(Channel.UNLIMITED)
        val receiveChannel = Channel<R>(Channel.UNLIMITED)

        scope.launch {
            try {
                // Wait for the first message (the subscription/request)
                val request = sendChannel.receive()

                val path = method.path
                val fullUrl = "http://localhost:50051/$path"
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
                            } catch (e: Exception) {
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
                        } catch (e: io.ktor.utils.io.errors.IOException) {
                            break
                        } catch (e: Exception) {
                            if (channel.isClosedForRead) break
                            throw e
                        }
                    }
                }
            } catch (t: Throwable) {
                receiveChannel.close(t)
            } finally {
                receiveChannel.close()
            }
        }

        return sendChannel to receiveChannel
    }

    override fun executeBlocking(): Pair<MessageSink<S>, MessageSource<R>> =
        throw UnsupportedOperationException("Blocking streaming not supported")

    override fun isCanceled(): Boolean = false

    override fun isExecuted(): Boolean = true

    override fun clone(): GrpcStreamingCall<S, R> = IosGrpcStreamingCall(client, method)

    override fun cancel() {}
}
