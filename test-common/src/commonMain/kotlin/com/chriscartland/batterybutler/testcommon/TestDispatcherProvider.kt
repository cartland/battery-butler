package com.chriscartland.batterybutler.testcommon

import com.chriscartland.batterybutler.domain.model.DispatcherProvider
import kotlinx.coroutines.CoroutineDispatcher

/**
 * A [DispatcherProvider] for tests. Point every dispatcher at a single test dispatcher (e.g.
 * `StandardTestDispatcher(testScheduler)` or `UnconfinedTestDispatcher(testScheduler)`) so code
 * that does `withContext(dispatcherProvider.io)` stays on the test scheduler's virtual clock and
 * runs deterministically — no real threads, no timing races.
 *
 * ```kotlin
 * val provider = TestDispatcherProvider(StandardTestDispatcher(testScheduler))
 * ```
 *
 * Pass distinct dispatchers when a test needs to tell IO/CPU/main work apart.
 */
class TestDispatcherProvider(
    override val default: CoroutineDispatcher,
    override val io: CoroutineDispatcher = default,
    override val main: CoroutineDispatcher = default,
) : DispatcherProvider
