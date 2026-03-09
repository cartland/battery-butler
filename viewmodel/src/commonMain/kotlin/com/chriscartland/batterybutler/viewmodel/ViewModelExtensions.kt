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
 * If the upstream flow throws an exception, it is caught and logged. When [onError] is
 * provided, the callback's return value is emitted so the UI can display an error state
 * instead of remaining stuck at [initialValue]. Without [onError], the flow simply
 * terminates (backward-compatible with previous behavior).
 */
fun <T> Flow<T>.safeStateIn(
    scope: CoroutineScope,
    started: SharingStarted,
    initialValue: T,
    onError: ((Throwable) -> T)? = null,
): StateFlow<T> =
    this
        .catch { e ->
            println("ViewModel safeStateIn caught unhandled exception: ${e.message}")
            e.printStackTrace()
            val errorValue = onError?.invoke(e)
            if (errorValue != null) {
                emit(errorValue)
            }
        }.stateIn(scope, started, initialValue)
