package com.chriscartland.batterybutler.domain.model

/**
 * Generic result type with constrained error type.
 *
 * This type allows callers to specify both the success data type and the error type,
 * enabling more precise error handling through the type system.
 *
 * Usage examples:
 * - `Result<User, DataError.Network>` - network operations returning User
 * - `Result<Config, DataError.Database>` - database operations returning Config
 * - `Result<String, DataError>` - any data error
 *
 * Project code NEVER throws exceptions except CancellationException.
 * Library exceptions are caught at data layer boundaries and returned as [Error].
 *
 * @param D The type of data on success.
 * @param E The type of error on failure, constrained to [AppError].
 */
sealed interface Result<out D, out E : AppError> {
    data class Success<D>(
        val data: D,
    ) : Result<D, Nothing>

    data class Error<E : AppError>(
        val error: E,
    ) : Result<Nothing, E>
}

/**
 * Maps the success data to a new type.
 */
inline fun <D, E : AppError, R> Result<D, E>.map(
    transform: (D) -> R,
): Result<R, E> =
    when (this) {
        is Result.Success -> Result.Success(transform(data))
        is Result.Error -> this
    }

/**
 * Maps the error to a new error type.
 */
inline fun <D, E : AppError, F : AppError> Result<D, E>.mapError(
    transform: (E) -> F,
): Result<D, F> =
    when (this) {
        is Result.Success -> this
        is Result.Error -> Result.Error(transform(error))
    }

/**
 * Returns the success data or null if this is an error.
 */
fun <D, E : AppError> Result<D, E>.getOrNull(): D? =
    when (this) {
        is Result.Success -> data
        is Result.Error -> null
    }

/**
 * Returns the success data or the result of [default] if this is an error.
 */
inline fun <D, E : AppError> Result<D, E>.getOrElse(default: (E) -> D): D =
    when (this) {
        is Result.Success -> data
        is Result.Error -> default(error)
    }

/**
 * Returns the success data or throws an [IllegalStateException] with the error message.
 */
fun <D, E : AppError> Result<D, E>.getOrThrow(): D =
    when (this) {
        is Result.Success -> data
        is Result.Error -> throw IllegalStateException(error.message)
    }

/**
 * Chains another result-returning operation if this result is successful.
 */
inline fun <D, E : AppError, R> Result<D, E>.flatMap(
    transform: (D) -> Result<R, E>,
): Result<R, E> =
    when (this) {
        is Result.Success -> transform(data)
        is Result.Error -> this
    }

/**
 * Executes [action] if this is a success.
 */
inline fun <D, E : AppError> Result<D, E>.onSuccess(action: (D) -> Unit): Result<D, E> {
    if (this is Result.Success) action(data)
    return this
}

/**
 * Executes [action] if this is an error.
 */
inline fun <D, E : AppError> Result<D, E>.onError(action: (E) -> Unit): Result<D, E> {
    if (this is Result.Error) action(error)
    return this
}
