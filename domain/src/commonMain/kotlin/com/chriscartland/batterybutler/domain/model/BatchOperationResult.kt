package com.chriscartland.batterybutler.domain.model

sealed interface BatchOperationResult {
    data class Progress(
        val message: String,
    ) : BatchOperationResult

    data class Success(
        val message: String,
    ) : BatchOperationResult

    data class Error(
        val error: DataError,
    ) : BatchOperationResult
}
