package com.chriscartland.batterybutler.experimental.model

import com.chriscartland.batterybutler.domain.model.AppError

sealed class CounterError : AppError {
    data class ReadFailed(
        override val message: String,
        override val cause: String? = null,
    ) : CounterError()

    data class WriteFailed(
        override val message: String,
        override val cause: String? = null,
    ) : CounterError()
}
