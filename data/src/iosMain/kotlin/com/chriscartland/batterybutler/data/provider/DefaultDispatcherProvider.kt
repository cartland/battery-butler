package com.chriscartland.batterybutler.data.provider

import com.chriscartland.batterybutler.domain.provider.DispatcherProvider
import kotlinx.coroutines.CoroutineDispatcher
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.IO
import me.tatarka.inject.annotations.Inject

@Inject
actual class DefaultDispatcherProvider : DispatcherProvider {
    actual override val main: CoroutineDispatcher = Dispatchers.Main
    actual override val io: CoroutineDispatcher = Dispatchers.IO
    actual override val default: CoroutineDispatcher = Dispatchers.Default
    actual override val unconfined: CoroutineDispatcher = Dispatchers.Unconfined
}
