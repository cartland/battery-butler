package com.chriscartland.batterybutler.datanetwork.grpc

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
    private val factory: (String) -> GrpcClient,
    private val networkModeRepository: NetworkModeRepository,
    private val scope: CoroutineScope,
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
                                GrpcClientState.Ready(factory(url))
                            } catch (e: Exception) {
                                if (e is CancellationException) throw e
                                e.printStackTrace()
                                GrpcClientState.Uninitialized // Or Error state?
                            }
                        }
                    }
                    is NetworkMode.GrpcAws -> {
                        val url = mode.url
                        if (url.isNullOrBlank()) {
                            GrpcClientState.InvalidConfiguration
                        } else {
                            try {
                                GrpcClientState.Ready(factory(url))
                            } catch (e: Exception) {
                                if (e is CancellationException) throw e
                                e.printStackTrace()
                                GrpcClientState.Uninitialized
                            }
                        }
                    }
                    NetworkMode.Mock -> GrpcClientState.Uninitialized // Mock doesn't use GrpcClient
                }
                currentDelegate.value = newClient
            }
        }
    }

    override fun <S : Any, R : Any> newCall(method: GrpcMethod<S, R>): GrpcCall<S, R> {
        val state = currentDelegate.value
        val delegate = (state as? GrpcClientState.Ready)?.client
            ?: throw IOException("Network client not ready. State: $state")
        return delegate.newCall(method)
    }

    override fun <S : Any, R : Any> newStreamingCall(method: GrpcMethod<S, R>): GrpcStreamingCall<S, R> {
        val state = currentDelegate.value
        val delegate = (state as? GrpcClientState.Ready)?.client
            ?: throw IOException("Network client not ready. State: $state")
        return delegate.newStreamingCall(method)
    }
}
