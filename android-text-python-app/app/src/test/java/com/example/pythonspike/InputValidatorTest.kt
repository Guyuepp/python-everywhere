package com.example.pythonspike

import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Test

class InputValidatorTest {
    @Test
    fun validate_null_returnsInvalidInput() {
        val result = InputValidator.validate(null)
        assertFalse(result.isValid)
        assertEquals("INVALID_INPUT", result.errorCode)
    }

    @Test
    fun validate_blank_returnsInvalidInput() {
        val result = InputValidator.validate("   \n\t")
        assertFalse(result.isValid)
        assertEquals("INVALID_INPUT", result.errorCode)
    }

    @Test
    fun validate_tooLong_returnsInvalidInput() {
        val result = InputValidator.validate("a".repeat(10_001))
        assertFalse(result.isValid)
        assertEquals("INVALID_INPUT", result.errorCode)
    }

    @Test
    fun validate_abnormalControlChar_returnsInvalidInput() {
        val result = InputValidator.validate("hello\u0000world")
        assertFalse(result.isValid)
        assertEquals("INVALID_INPUT", result.errorCode)
    }

    @Test
    fun validate_normalText_returnsValid() {
        val result = InputValidator.validate("  hello world  ")
        assertTrue(result.isValid)
        assertEquals("hello world", result.normalizedText)
    }
}
