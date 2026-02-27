package com.chriscartland.batterybutler.viewmodel

import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.catch
import kotlinx.coroutines.flow.stateIn

/**
 * Default timeout (in milliseconds) for [SharingStarted.WhileSubscribed] in ViewModels.
 *
 * This keeps the upstream flow active for 5 seconds after the last subscriber disconnects,
 * which prevents unnecessary recomputation during configuration changes (like screen rotation)
 * while still allowing the flow to be properly cancelled when no longer needed.
 */
const val DEFAULT_WHILE_SUBSCRIBED_TIMEOUT_MS = 5_000L

/**
 * Creates a [SharingStarted.WhileSubscribed] with the default timeout.
 *
 * Use this for consistent behavior across ViewModels.
 */
fun defaultWhileSubscribed(): SharingStarted = SharingStarted.WhileSubscribed(DEFAULT_WHILE_SUBSCRIBED_TIMEOUT_MS)

/**
 * Safely converts a Flow into a StateFlow that prevents unhandled exceptions from
 * crashing the host application (specifically on iOS where unhandled coroutine
 * exceptions are fatal).
 *
 * If the upstream flow throws an exception, it is caught, logged, and
 * the flow terminates, leaving the StateFlow with its last emitted value (or initialValue).
 */
fun <T> Flow<T>.safeStateIn(
    scope: CoroutineScope,
    started: SharingStarted,
    initialValue: T,
): StateFlow<T> =
    this
        .catch { e ->
            println("ViewModel safeStateIn caught unhandled exception: ${e.message}")
            e.printStackTrace()
        }.stateIn(scope, started, initialValue)
