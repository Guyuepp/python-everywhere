package com.example.pythonspike

import org.junit.Assert.assertEquals
import org.junit.Assert.assertNull
import org.junit.Test

class ExecutionHistoryRepositoryTest {
    @Test
    fun trimToLimit_keepsMostRecentItems() {
        val allItems = (1L..120L).map { index ->
            ExecutionHistoryItem(
                createdAtEpochMs = index,
                requestId = "req-$index",
                status = "success",
                errorCode = null,
                message = "ok",
                inputPreview = "input-$index",
                durationMs = index,
                source = "PROCESS_TEXT"
            )
        }

        val trimmed = ExecutionHistoryRepository.trimToLimit(allItems, 100)

        assertEquals(100, trimmed.size)
        assertEquals(120L, trimmed.first().createdAtEpochMs)
        assertEquals(21L, trimmed.last().createdAtEpochMs)
    }

    @Test
    fun encodeItems_decodeItems_roundTripsNullableFields() {
        val original = listOf(
            ExecutionHistoryItem(
                createdAtEpochMs = 200L,
                requestId = "req-200",
                status = "error",
                errorCode = "PYTHON_RUNTIME_ERROR",
                message = "boom",
                inputPreview = "1/0",
                durationMs = 30L,
                source = "PROCESS_TEXT"
            ),
            ExecutionHistoryItem(
                createdAtEpochMs = 100L,
                requestId = "req-100",
                status = "success",
                errorCode = null,
                message = "ok",
                inputPreview = "1+2",
                durationMs = 10L,
                source = "PROCESS_TEXT"
            )
        )

        val encoded = ExecutionHistoryRepository.encodeItems(original)
        val decoded = ExecutionHistoryRepository.decodeItems(encoded)

        assertEquals(original.size, decoded.size)
        assertEquals(original[0], decoded[0])
        assertEquals(original[1], decoded[1])
        assertNull(decoded[1].errorCode)
    }
}
