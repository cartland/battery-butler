package com.chriscartland.batterybutler.domain.model

import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertFailsWith
import kotlin.test.assertIs
import kotlin.test.assertNull
import kotlin.test.assertTrue

class ResultTest {
    private fun successResult(data: String = "hello"): Result<String, TestError> = Result.Success(data)

    private fun errorResult(msg: String = "fail"): Result<String, TestError> = Result.Error(TestError(msg))

    // -- map --

    @Test
    fun `map transforms success value`() {
        val result = successResult("abc").map { it.length }

        assertIs<Result.Success<Int>>(result)
        assertEquals(3, result.data)
    }

    @Test
    fun `map preserves error unchanged`() {
        val original = errorResult("broken")
        val result = original.map { it.length }

        assertIs<Result.Error<TestError>>(result)
        assertEquals("broken", result.error.message)
    }

    // -- mapError --

    @Test
    fun `mapError transforms error value`() {
        val result = errorResult("original").mapError { TestError("mapped: ${it.message}") }

        assertIs<Result.Error<TestError>>(result)
        assertEquals("mapped: original", result.error.message)
    }

    @Test
    fun `mapError preserves success unchanged`() {
        val result = successResult("data").mapError { TestError("should not happen") }

        assertIs<Result.Success<String>>(result)
        assertEquals("data", result.data)
    }

    // -- flatMap --

    @Test
    fun `flatMap chains on success`() {
        val result = successResult("hello").flatMap { Result.Success(it.uppercase()) }

        assertIs<Result.Success<String>>(result)
        assertEquals("HELLO", result.data)
    }

    @Test
    fun `flatMap short-circuits on error`() {
        var called = false
        val result = errorResult("fail").flatMap {
            called = true
            Result.Success(it.uppercase())
        }

        assertIs<Result.Error<TestError>>(result)
        assertEquals("fail", result.error.message)
        assertEquals(false, called)
    }

    @Test
    fun `flatMap propagates transform error`() {
        val result = successResult("hello").flatMap<String, TestError, Int> {
            Result.Error(TestError("transform failed"))
        }

        assertIs<Result.Error<TestError>>(result)
        assertEquals("transform failed", result.error.message)
    }

    // -- getOrNull --

    @Test
    fun `getOrNull returns data on success`() {
        assertEquals("hello", successResult("hello").getOrNull())
    }

    @Test
    fun `getOrNull returns null on error`() {
        assertNull(errorResult().getOrNull())
    }

    // -- getOrElse --

    @Test
    fun `getOrElse returns data on success`() {
        val result = successResult("data").getOrElse { "default" }

        assertEquals("data", result)
    }

    @Test
    fun `getOrElse returns default on error`() {
        val result = errorResult("oops").getOrElse { "fallback-${it.message}" }

        assertEquals("fallback-oops", result)
    }

    // -- getOrThrow --

    @Test
    fun `getOrThrow returns data on success`() {
        assertEquals("hello", successResult("hello").getOrThrow())
    }

    @Test
    fun `getOrThrow throws IllegalStateException on error`() {
        val exception = assertFailsWith<IllegalStateException> {
            errorResult("boom").getOrThrow()
        }
        assertEquals("boom", exception.message)
    }

    // -- onSuccess --

    @Test
    fun `onSuccess executes callback on success`() {
        var captured: String? = null
        successResult("value").onSuccess { captured = it }

        assertEquals("value", captured)
    }

    @Test
    fun `onSuccess skips callback on error`() {
        var called = false
        errorResult().onSuccess { called = true }

        assertEquals(false, called)
    }

    @Test
    fun `onSuccess returns same result`() {
        val original = successResult("data")
        val returned = original.onSuccess { }

        assertTrue(original === returned)
    }

    // -- onError --

    @Test
    fun `onError executes callback on error`() {
        var captured: String? = null
        errorResult("problem").onError { captured = it.message }

        assertEquals("problem", captured)
    }

    @Test
    fun `onError skips callback on success`() {
        var called = false
        successResult().onError { called = true }

        assertEquals(false, called)
    }

    @Test
    fun `onError returns same result`() {
        val original = errorResult()
        val returned = original.onError { }

        assertTrue(original === returned)
    }
}

private data class TestError(
    override val message: String = "test error",
    override val cause: String? = null,
) : AppError
