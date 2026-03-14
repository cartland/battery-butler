package com.chriscartland.batterybutler.experimental.usecase

import com.chriscartland.batterybutler.experimental.domain.service.AppScopedCounter
import me.tatarka.inject.annotations.Inject

// @NoTestRequired: Trivial single-method delegate to AppScopedCounter.stop()
@Inject
class StopAppScopedCounterUseCase(
    private val appScopedCounter: AppScopedCounter,
) {
    operator fun invoke() {
        appScopedCounter.stop()
    }
}
