package com.chriscartland.batterybutler.domain.repository

import kotlinx.coroutines.flow.MutableSharedFlow
import kotlinx.coroutines.flow.SharedFlow
import kotlinx.coroutines.flow.asSharedFlow

/**
 * Long-lived restart-request bus, scoped to the AppComponent singleton.
 *
 * Lets a ViewModel (or any layer) request an app process restart without
 * depending on its own composition scope (or its own lifecycle) surviving
 * until the restart is acted on — see bd issue bb-lg42.
 *
 * Producer: [com.chriscartland.batterybutler.viewmodel.settings.SettingsViewModel]
 *           calls [requestRestart] after a successful Restore Previous Data.
 *
 * Consumer: the root `App` composable holds a `LaunchedEffect(Unit)` that
 *           collects [events], delays briefly (so the snackbar can render),
 *           and invokes the platform restart hook. Because the root composable
 *           lives as long as the host Activity, the collector is durable
 *           across navigation, ViewModel teardown, and back-stack changes —
 *           fixing the android/33 failure mode where the user tapped Devices
 *           immediately after Restore and the in-Settings LaunchedEffect was
 *           cancelled before its delay fired.
 *
 * Buffer / replay choices:
 *   - `extraBufferCapacity = 1` so [requestRestart] never suspends and can
 *     be called from a non-suspending context.
 *   - `replay = 0` so a restart isn't re-fired if a new collector subscribes
 *     after the event has already been consumed. The App-root collector is
 *     set up at app start, so this is defensive only.
 */
class RestartCoordinator {
    private val _events = MutableSharedFlow<Unit>(replay = 0, extraBufferCapacity = 1)
    val events: SharedFlow<Unit> = _events.asSharedFlow()

    /**
     * Request an app process restart. Non-suspending; emission is buffered if
     * no collector is currently subscribed.
     */
    fun requestRestart() {
        _events.tryEmit(Unit)
    }
}
