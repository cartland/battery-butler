package com.chriscartland.batterybutler.viewmodel

import kotlinx.coroutines.flow.SharingStarted

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
