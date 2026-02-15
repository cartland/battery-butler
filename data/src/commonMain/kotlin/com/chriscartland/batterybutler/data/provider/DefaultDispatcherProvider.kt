package com.chriscartland.batterybutler.data.provider

import com.chriscartland.batterybutler.domain.provider.DispatcherProvider
import kotlinx.coroutines.CoroutineDispatcher
import me.tatarka.inject.annotations.Inject

@Inject
expect class DefaultDispatcherProvider() : DispatcherProvider {
    override val main: CoroutineDispatcher
    override val io: CoroutineDispatcher
    override val default: CoroutineDispatcher
    override val unconfined: CoroutineDispatcher
}
