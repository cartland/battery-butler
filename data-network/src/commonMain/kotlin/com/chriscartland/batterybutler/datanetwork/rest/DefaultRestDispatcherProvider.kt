package com.chriscartland.batterybutler.datanetwork.rest

import com.chriscartland.batterybutler.domain.model.DispatcherProvider
import kotlinx.coroutines.CoroutineDispatcher
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.IO

/**
 * Fallback [DispatcherProvider] for REST data sources constructed without DI (standalone / tests
 * that don't care which dispatcher is used). Production always injects the shared provider through
 * the Delegating* sources, so long downloads run on the app's real, elastic IO pool and unit tests
 * can substitute a test dispatcher for deterministic virtual-time control.
 */
internal object DefaultRestDispatcherProvider : DispatcherProvider {
    override val default: CoroutineDispatcher = Dispatchers.Default
    override val io: CoroutineDispatcher = Dispatchers.IO
    override val main: CoroutineDispatcher = Dispatchers.Main
}
