package com.example.pythonspike

import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertNull
import org.junit.Assert.assertTrue
import org.junit.Test

class ProcessTextIntentReaderTest {
    @Test
    fun read_processText_success() {
        val outcome = ProcessTextIntentReader.read(
            action = "android.intent.action.PROCESS_TEXT",
            readProcessText = { " hello " },
            readDebugDelayMs = { 50L },
            readDebugFail = { true },
            isIntentTooLargeError = { false }
        )

        assertNull(outcome.errorCode)
        assertEquals(" hello ", outcome.input)
        assertEquals(50L, outcome.debugDelayMs)
        assertTrue(outcome.debugFail)
    }

    @Test
    fun read_nonProcessText_usesDefaultHello() {
        val outcome = ProcessTextIntentReader.read(
            action = "android.intent.action.MAIN",
            readProcessText = { "ignored" },
            readDebugDelayMs = { 0L },
            readDebugFail = { false },
            isIntentTooLargeError = { false }
        )

        assertNull(outcome.errorCode)
        assertEquals("'hello'", outcome.input)
        assertEquals(0L, outcome.debugDelayMs)
        assertFalse(outcome.debugFail)
    }

    @Test
    fun read_runtimeError_mapsToInvalidInput() {
        val outcome = ProcessTextIntentReader.read(
            action = "android.intent.action.PROCESS_TEXT",
            readProcessText = { throw IllegalStateException("boom") },
            readDebugDelayMs = { 0L },
            readDebugFail = { false },
            isIntentTooLargeError = { false }
        )

        assertEquals("INVALID_INPUT", outcome.errorCode)
        assertEquals("Failed to read input", outcome.message)
    }

    @Test
    fun read_transactionTooLarge_mapsToIntentTooLarge() {
        val error = RuntimeException("FAILED BINDER TRANSACTION")
        val outcome = ProcessTextIntentReader.read(
            action = "android.intent.action.PROCESS_TEXT",
            readProcessText = { throw error },
            readDebugDelayMs = { 0L },
            readDebugFail = { false },
            isIntentTooLargeError = { throwable -> throwable === error }
        )

        assertEquals("INTENT_TOO_LARGE", outcome.errorCode)
        assertEquals("Input is too large for intent transport", outcome.message)
    }
}
