package com.chriscartland.batterybutler.domain.model

import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertIs
import kotlin.test.assertNotEquals
import kotlin.test.assertTrue

class BatchOperationResultTest {
    @Test
    fun `Progress stores message`() {
        val progress = BatchOperationResult.Progress(message = "Processing item 1 of 10")

        assertEquals("Processing item 1 of 10", progress.message)
    }

    @Test
    fun `Progress instances with same message are equal`() {
        val progress1 = BatchOperationResult.Progress(message = "Processing")
        val progress2 = BatchOperationResult.Progress(message = "Processing")

        assertEquals(progress1, progress2)
        assertEquals(progress1.hashCode(), progress2.hashCode())
    }

    @Test
    fun `Progress instances with different messages are not equal`() {
        val progress1 = BatchOperationResult.Progress(message = "Step 1")
        val progress2 = BatchOperationResult.Progress(message = "Step 2")

        assertNotEquals(progress1, progress2)
    }

    @Test
    fun `Success stores message`() {
        val success = BatchOperationResult.Success(message = "Operation completed successfully")

        assertEquals("Operation completed successfully", success.message)
    }

    @Test
    fun `Success instances with same message are equal`() {
        val success1 = BatchOperationResult.Success(message = "Done")
        val success2 = BatchOperationResult.Success(message = "Done")

        assertEquals(success1, success2)
        assertEquals(success1.hashCode(), success2.hashCode())
    }

    @Test
    fun `Error stores typed error with message`() {
        val error = BatchOperationResult.Error(
            error = DataError.Ai.ApiError("Failed to process item"),
        )

        assertEquals("Failed to process item", error.error.message)
    }

    @Test
    fun `Error instances with same error are equal`() {
        val error1 = BatchOperationResult.Error(
            error = DataError.Ai.ApiError("Error occurred"),
        )
        val error2 = BatchOperationResult.Error(
            error = DataError.Ai.ApiError("Error occurred"),
        )

        assertEquals(error1, error2)
        assertEquals(error1.hashCode(), error2.hashCode())
    }

    @Test
    fun `sealed interface variants are distinguishable`() {
        val progress: BatchOperationResult = BatchOperationResult.Progress("progress")
        val success: BatchOperationResult = BatchOperationResult.Success("success")
        val error: BatchOperationResult = BatchOperationResult.Error(DataError.Unknown("error"))

        assertIs<BatchOperationResult.Progress>(progress)
        assertIs<BatchOperationResult.Success>(success)
        assertIs<BatchOperationResult.Error>(error)
    }

    @Test
    fun `when expression covers all variants`() {
        val results = listOf(
            BatchOperationResult.Progress("progress"),
            BatchOperationResult.Success("success"),
            BatchOperationResult.Error(DataError.Unknown("error")),
        )

        for (result in results) {
            val description = when (result) {
                is BatchOperationResult.Progress -> "in progress"
                is BatchOperationResult.Success -> "succeeded"
                is BatchOperationResult.Error -> "failed"
            }
            // If we get here without error, the when is exhaustive
            assertTrue(description.isNotEmpty())
        }
    }

    @Test
    fun `different variant types are not equal`() {
        val progress = BatchOperationResult.Progress("test")
        val success = BatchOperationResult.Success("test")
        val error = BatchOperationResult.Error(DataError.Unknown("test"))

        assertNotEquals<BatchOperationResult>(progress, success)
        assertNotEquals<BatchOperationResult>(success, error)
        assertNotEquals<BatchOperationResult>(progress, error)
    }

    @Test
    fun `error types are exhaustively matchable`() {
        val error = BatchOperationResult.Error(DataError.Ai.ParsingError("Parse failed"))

        val errorDescription = when (error.error) {
            is DataError.Network.ConnectionFailed -> "connection"
            is DataError.Network.Timeout -> "timeout"
            is DataError.Network.ServerError -> "server"
            is DataError.Network.NotReady -> "not ready"
            is DataError.Network.PushFailed -> "push failed"
            is DataError.Database.ReadFailed -> "db read"
            is DataError.Database.WriteFailed -> "db write"
            is DataError.Database.ConstraintViolation -> "constraint"
            is DataError.Ai.ApiError -> "ai api"
            is DataError.Ai.ParsingError -> "ai parse"
            is DataError.Unknown -> "unknown"
        }

        assertEquals("ai parse", errorDescription)
    }
}
