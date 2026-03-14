package com.chriscartland.batterybutler.experimental.domain.service

import kotlinx.coroutines.flow.StateFlow

interface AppCounterService {
    val isRunning: StateFlow<Boolean>

    fun start()

    fun stop()
}
