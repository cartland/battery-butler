package com.chriscartland.batterybutler.viewmodel

import com.rickclephas.kmp.observableviewmodel.ViewModelScope
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.catch
import kotlinx.coroutines.flow.flatMapLatest
import kotlinx.coroutines.flow.stateIn
import com.rickclephas.kmp.observableviewmodel.stateIn as observableStateIn

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

/**
 * Observable variant of [safeStateIn] for KMP-ObservableViewModel (bb-ovm1 spike).
 *
 * Identical iOS-safe `.catch` behavior, but routes the flow through
 * KMP-ObservableViewModel's [stateIn][observableStateIn] (which takes the ViewModel's
 * [ViewModelScope]). The resulting StateFlow is an `ObservableStateFlow` registered with
 * the ViewModel's SwiftUI change publisher, so SwiftUI `@StateViewModel`/`@ObservedViewModel`
 * re-render automatically on emission — no manual `Task { for await }` bridge needed.
 *
 * Use this overload (passing `viewModelScope` directly) for ViewModels consumed by the
 * native SwiftUI app; the [CoroutineScope] overload remains for Compose-only paths.
 */
fun <T> Flow<T>.safeStateIn(
    viewModelScope: ViewModelScope,
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
        }.observableStateIn(viewModelScope, started, initialValue)

/**
 * StateFlow with retry support. Like [safeStateIn] but re-subscribes to the source flow
 * every time [retryTrigger] emits, which resets any prior error state. Use this for
 * screens that surface a user-visible "retry" button when their data fails to load.
 *
 * The [source] factory is invoked on every retry, so subscribers get a fresh chain
 * (combine, map, etc.) and the [catch] inside resets too. Errors are caught and
 * converted to a state value via [onError]; subsequent retries can recover from them.
 *
 * Typical wiring:
 * ```
 * private val retryTrigger = MutableStateFlow(0)
 *
 * val uiState: StateFlow<MyScreenState> = retryableStateIn(
 *     scope = viewModelScope,
 *     retryTrigger = retryTrigger,
 *     started = defaultWhileSubscribed(),
 *     initialValue = MyScreenState.Loading,
 *     onError = { MyScreenState.Error(it.message ?: "Failed to load") },
 *     source = { combine(flowA(), flowB()) { a, b -> MyScreenState.Success(a, b) } },
 * )
 *
 * fun retry() { retryTrigger.update { it + 1 } }
 * ```
 */
@OptIn(ExperimentalCoroutinesApi::class)
fun <T> retryableStateIn(
    scope: CoroutineScope,
    retryTrigger: Flow<*>,
    started: SharingStarted,
    initialValue: T,
    onError: (Throwable) -> T,
    source: () -> Flow<T>,
): StateFlow<T> =
    retryTrigger
        .flatMapLatest {
            source()
                .catch { e ->
                    println("ViewModel retryableStateIn caught: ${e.message}")
                    e.printStackTrace()
                    emit(onError(e))
                }
        }.stateIn(scope, started, initialValue)

/**
 * Observable variant of [retryableStateIn] for KMP-ObservableViewModel (bb-ovm1).
 *
 * Same retry semantics, but routes the result through KMP-ObservableViewModel's observable
 * [stateIn][observableStateIn] so the exposed StateFlow drives the SwiftUI change publisher
 * (`@StateViewModel`/`@ObservedViewModel` auto-update). Pass the ViewModel's [ViewModelScope].
 */
@OptIn(ExperimentalCoroutinesApi::class)
fun <T> retryableStateIn(
    viewModelScope: ViewModelScope,
    retryTrigger: Flow<*>,
    started: SharingStarted,
    initialValue: T,
    onError: (Throwable) -> T,
    source: () -> Flow<T>,
): StateFlow<T> =
    retryTrigger
        .flatMapLatest {
            source()
                .catch { e ->
                    println("ViewModel retryableStateIn caught: ${e.message}")
                    e.printStackTrace()
                    emit(onError(e))
                }
        }.observableStateIn(viewModelScope, started, initialValue)
