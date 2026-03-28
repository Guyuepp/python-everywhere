package com.example.pythonspike

import org.json.JSONObject
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Test

class ResultPayloadTest {
    @Test
    fun toJsonString_successPayload_hasStableFields() {
        val execution = PythonResultPayload(
            requestId = "req-success",
            status = "success",
            errorCode = null,
            message = "ok",
            resultText = "python_ok:hello",
            stdout = "",
            stderr = "",
            error = null,
            traceback = null,
            isColdStart = true
        )

        val json = JSONObject(ResultPayload.fromExecution(execution, durationMs = 123).toJsonString())

        assertEquals("process_text_result", json.getString("protocol"))
        assertEquals("1.0.0", json.getString("protocolVersion"))
        assertEquals("req-success", json.getString("requestId"))
        assertEquals("success", json.getString("status"))
        assertTrue(json.isNull("errorCode"))
        assertEquals("ok", json.getString("message"))
        assertEquals("python_ok:hello", json.getJSONObject("result").getString("text"))
        assertEquals("", json.getString("stdout"))
        assertEquals("", json.getString("stderr"))
        assertTrue(json.isNull("traceback"))
        assertTrue(json.getBoolean("isColdStart"))
        assertEquals(123L, json.getLong("durationMs"))
    }

    @Test
    fun toJsonString_errorPayload_containsErrorCode() {
        val execution = PythonResultPayload.fromThrowable(
            error = IllegalStateException("boom"),
            requestId = "req-error",
            isColdStart = false
        )

        val json = JSONObject(ResultPayload.fromExecution(execution, durationMs = 80).toJsonString())

        assertEquals("error", json.getString("status"))
        assertEquals("PYTHON_RUNTIME_ERROR", json.getString("errorCode"))
        assertEquals("req-error", json.getString("requestId"))
        assertTrue(json.isNull("result"))
        assertFalse(json.getBoolean("isColdStart"))
        assertEquals(80L, json.getLong("durationMs"))
    }

    @Test
    fun toJsonString_cancelledPayload_containsCancelledStatus() {
        val execution = PythonResultPayload.cancelled(
            requestId = "req-cancel",
            reason = "cancelled by host",
            isColdStart = false
        )

        val json = JSONObject(ResultPayload.fromExecution(execution, durationMs = 41).toJsonString())

        assertEquals("cancelled", json.getString("status"))
        assertEquals("PYTHON_CANCELLED", json.getString("errorCode"))
        assertEquals("cancelled by host", json.getString("message"))
        assertTrue(json.isNull("result"))
        assertEquals(41L, json.getLong("durationMs"))
    }

    @Test
    fun toJsonString_timeoutPayload_containsTimeoutCode() {
        val execution = PythonResultPayload.timeout(
            requestId = "req-timeout",
            timeoutMs = 700,
            isColdStart = true
        )

        val json = JSONObject(ResultPayload.fromExecution(execution, durationMs = 700).toJsonString())

        assertEquals("error", json.getString("status"))
        assertEquals("PYTHON_EXEC_TIMEOUT", json.getString("errorCode"))
        assertTrue(json.getString("message").contains("timed out"))
        assertTrue(json.isNull("result"))
        assertTrue(json.getBoolean("isColdStart"))
        assertEquals(700L, json.getLong("durationMs"))
    }
}
