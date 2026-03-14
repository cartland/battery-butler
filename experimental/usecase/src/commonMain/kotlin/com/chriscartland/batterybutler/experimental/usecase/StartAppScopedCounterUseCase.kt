package com.chriscartland.batterybutler.experimental.usecase

import com.chriscartland.batterybutler.experimental.domain.service.AppScopedCounter
import me.tatarka.inject.annotations.Inject

// @NoTestRequired: Trivial single-method delegate to AppScopedCounter.start()
@Inject
class StartAppScopedCounterUseCase(
    private val appScopedCounter: AppScopedCounter,
) {
    operator fun invoke() {
        appScopedCounter.start()
    }
}
