package com.chriscartland.batterybutler.datanetwork.grpc

import co.touchlab.kermit.Logger
import com.chriscartland.batterybutler.domain.model.DispatcherProvider
import com.chriscartland.batterybutler.domain.model.NetworkMode
import com.chriscartland.batterybutler.domain.repository.NetworkModeRepository
import com.squareup.wire.GrpcCall
import com.squareup.wire.GrpcClient
import com.squareup.wire.GrpcMethod
import com.squareup.wire.GrpcStreamingCall
import kotlinx.coroutines.CancellationException
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.launch
import okio.IOException

sealed interface GrpcClientState {
    data object Uninitialized : GrpcClientState

    data object InvalidConfiguration : GrpcClientState

    data class Ready(
        val client: GrpcClient,
    ) : GrpcClientState
}

class DelegatingGrpcClient(
    private val factory: (String, DispatcherProvider) -> GrpcClient,
    private val networkModeRepository: NetworkModeRepository,
    private val scope: CoroutineScope,
    private val dispatcherProvider: DispatcherProvider,
    private val tokenProvider: (() -> String?)? = null,
) : GrpcClient() {
    private val currentDelegate = MutableStateFlow<GrpcClientState>(GrpcClientState.Uninitialized)
    val clientState: StateFlow<GrpcClientState> = currentDelegate

    init {
        scope.launch {
            networkModeRepository.networkMode.collect { mode ->
                val newClient = when (mode) {
                    is NetworkMode.GrpcLocal -> {
                        val url = mode.url
                        if (url.isNullOrBlank()) {
                            GrpcClientState.InvalidConfiguration
                        } else {
                            try {
                                GrpcClientState.Ready(factory(url, dispatcherProvider))
                            } catch (e: Exception) {
                                if (e is CancellationException) throw e
                                Logger.e("DelegatingGrpcClient", e) { "Failed to create gRPC client" }
                                GrpcClientState.Uninitialized // Or Error state?
                            }
                        }
                    }

                    is NetworkMode.GrpcAws, is NetworkMode.GrpcDev -> {
                        @Suppress("ElseCaseInsteadOfExhaustiveWhen")
                        val url = when (mode) {
                            is NetworkMode.GrpcAws -> mode.url
                            is NetworkMode.GrpcDev -> mode.url
                            else -> null // Intentional: nested inside GrpcAws/GrpcDev branch
                        }
                        if (url.isNullOrBlank()) {
                            GrpcClientState.InvalidConfiguration
                        } else {
                            try {
                                GrpcClientState.Ready(factory(url, dispatcherProvider))
                            } catch (e: Exception) {
                                if (e is CancellationException) throw e
                                Logger.e("DelegatingGrpcClient", e) { "Failed to create gRPC client" }
                                GrpcClientState.Uninitialized
                            }
                        }
                    }

                    NetworkMode.Mock, NetworkMode.None -> {
                        GrpcClientState.Uninitialized
                    } // Mock and None don't use GrpcClient
                }
                currentDelegate.value = newClient
            }
        }
    }

    override fun <S : Any, R : Any> newCall(method: GrpcMethod<S, R>): GrpcCall<S, R> {
        val state = currentDelegate.value
        val delegate = (state as? GrpcClientState.Ready)?.client
            ?: throw IOException("Network client not ready. State: $state")
        val call = delegate.newCall(method)
        call.requestMetadata = call.requestMetadata + authHeaders()
        return call
    }

    override fun <S : Any, R : Any> newStreamingCall(method: GrpcMethod<S, R>): GrpcStreamingCall<S, R> {
        val state = currentDelegate.value
        val delegate = (state as? GrpcClientState.Ready)?.client
            ?: throw IOException("Network client not ready. State: $state")
        val call = delegate.newStreamingCall(method)
        call.requestMetadata = call.requestMetadata + authHeaders()
        return call
    }

    private fun authHeaders(): Map<String, String> {
        val token = tokenProvider?.invoke() ?: return emptyMap()
        return mapOf("authorization" to "Bearer $token")
    }
}
