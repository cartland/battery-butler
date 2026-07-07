package com.chriscartland.batterybutler.domain.model

import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.distinctUntilChanged
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.flow.flatMapLatest
import kotlinx.coroutines.flow.map

/**
 * Holds one value of [T] per distinct key derived from [NetworkMode] (e.g. one Labs auth session
 * per Firebase project), and reactively exposes whichever value belongs to the network mode
 * that's selected *right now*.
 *
 * This exists to make a specific class of bug structurally impossible: a plain
 * `MutableStateFlow<T>` used for per-environment session state (auth status, cached tokens, ...)
 * has no way to know the environment changed, so it keeps showing whatever the *previous*
 * environment left behind -- e.g. Settings displaying "signed in" for Labs staging right after
 * switching modes, when only prod was ever actually authenticated (see `bb-labs-mode-auth-state`
 * in TODO.md for the incident this was extracted from). [current] can't exhibit that bug: there
 * is no unpartitioned "current value" field to go stale -- every read re-derives the key from the
 * live [networkMode] flow.
 *
 * Use this instead of a bare `MutableStateFlow` whenever new state depends on "which backend is
 * selected right now" (staging vs prod, or any future per-environment concern).
 */
class NetworkModeKeyedState<T>(
    private val networkMode: Flow<NetworkMode>,
    private val keyFor: (NetworkMode) -> String,
    private val default: T,
) {
    private val statesByKey = mutableMapOf<String, MutableStateFlow<T>>()

    private fun stateFor(key: String): MutableStateFlow<T> = statesByKey.getOrPut(key) { MutableStateFlow(default) }

    /** Reactively follows whichever key the current network mode maps to. */
    @OptIn(ExperimentalCoroutinesApi::class)
    val current: Flow<T> =
        networkMode
            .map(keyFor)
            .distinctUntilChanged()
            .flatMapLatest { key -> stateFor(key) }

    /** Sets the value for the environment that is current *right now*. */
    suspend fun setCurrent(value: T) {
        stateFor(keyFor(networkMode.first())).value = value
    }

    /** Reads-and-writes the value for the environment that is current *right now*. */
    suspend fun updateCurrent(transform: (T) -> T) {
        val key = keyFor(networkMode.first())
        val flow = stateFor(key)
        flow.value = transform(flow.value)
    }

    /**
     * Sets the value for [key] only if its current value equals [expected] -- used to resolve a
     * placeholder default (e.g. from an async read of persisted state) without clobbering a real
     * transition that already happened for that key in the meantime.
     *
     * @return true if the value was actually set (i.e. it still equaled [expected]).
     */
    fun compareAndSet(
        key: String,
        expected: T,
        newValue: T,
    ): Boolean {
        val flow = stateFor(key)
        if (flow.value != expected) return false
        flow.value = newValue
        return true
    }
}
