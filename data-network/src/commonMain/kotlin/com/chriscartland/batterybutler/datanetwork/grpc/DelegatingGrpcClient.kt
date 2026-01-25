package com.chriscartland.batterybutler.datanetwork.grpc

import com.chriscartland.batterybutler.domain.model.NetworkMode
import com.chriscartland.batterybutler.domain.repository.NetworkModeRepository
import com.squareup.wire.GrpcCall
import com.squareup.wire.GrpcClient
import com.squareup.wire.GrpcMethod
import com.squareup.wire.GrpcStreamingCall
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.launch
import okio.IOException

class DelegatingGrpcClient(
    private val factory: () -> GrpcClient,
    private val networkModeRepository: NetworkModeRepository,
    private val scope: CoroutineScope,
) : GrpcClient() {
    private val currentDelegate = MutableStateFlow<GrpcClient?>(null)
    val clientState: StateFlow<GrpcClient?> = currentDelegate

    init {
        scope.launch {
            networkModeRepository.networkMode.collect { mode ->
                // Close previous client if exists?
                // GrpcClient usually doesn't have a standardized close() method in common,
                // but underlying implementations might.
                // Since this object is kept alive, we just drop the reference.
                // If the underlying client needs disposal, we assume GC or specific checks.
                // Android GrpcClient uses OkHttp which should be closed usually, but here we rebuild it.
                // Ideally we'd cast and close if possible.

                val newClient = when (mode) {
                    NetworkMode.GRPC_LOCAL -> {
                        // Enforce local address for Android Emulator
                        SharedServerConfig.setServerUrl(SharedServerConfig.LOCAL_GRPC_ADDRESS_ANDROID)
                        try {
                            factory()
                        } catch (e: Exception) {
                            if (e is kotlinx.coroutines.CancellationException) throw e
                            e.printStackTrace()
                            null
                        }
                    }
                    NetworkMode.GRPC_AWS -> {
                        // Enforce default production URL
                        SharedServerConfig.resetServerUrl()
                        try {
                            factory()
                        } catch (e: Exception) {
                            if (e is kotlinx.coroutines.CancellationException) throw e
                            e.printStackTrace()
                            null
                        }
                    }
                    NetworkMode.MOCK -> null
                }
                currentDelegate.value = newClient
            }
        }
    }

    override fun <S : Any, R : Any> newCall(method: GrpcMethod<S, R>): GrpcCall<S, R> {
        val delegate = currentDelegate.value ?: throw IOException("Network disabled or Mock mode active")
        return delegate.newCall(method)
    }

    override fun <S : Any, R : Any> newStreamingCall(method: GrpcMethod<S, R>): GrpcStreamingCall<S, R> {
        val delegate = currentDelegate.value ?: throw IOException("Network disabled or Mock mode active")
        return delegate.newStreamingCall(method)
    }
}
