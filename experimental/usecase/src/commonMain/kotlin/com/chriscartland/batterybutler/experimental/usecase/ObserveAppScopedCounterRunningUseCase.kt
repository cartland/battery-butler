package com.chriscartland.batterybutler.experimental.usecase

import com.chriscartland.batterybutler.experimental.domain.service.AppScopedCounter
import kotlinx.coroutines.flow.StateFlow
import me.tatarka.inject.annotations.Inject

// @NoTestRequired: Trivial single-property delegate to AppScopedCounter.isRunning
@Inject
class ObserveAppScopedCounterRunningUseCase(
    private val appScopedCounter: AppScopedCounter,
) {
    operator fun invoke(): StateFlow<Boolean> = appScopedCounter.isRunning
}
