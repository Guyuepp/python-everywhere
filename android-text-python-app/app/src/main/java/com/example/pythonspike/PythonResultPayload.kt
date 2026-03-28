package com.example.pythonspike

import org.json.JSONObject

data class PythonResultPayload(
    val requestId: String,
    val status: String,
    val errorCode: String?,
    val message: String,
    val resultText: String?,
    val stdout: String,
    val stderr: String,
    val error: String?,
    val traceback: String?,
    val isColdStart: Boolean
) {
    val isSuccess: Boolean
        get() = status == "success" && errorCode.isNullOrBlank() && error.isNullOrBlank()

    val isCancelled: Boolean
        get() = status == "cancelled" && errorCode == "PYTHON_CANCELLED"

    companion object {
        fun fromJson(rawJson: String, isColdStart: Boolean, fallbackRequestId: String): PythonResultPayload {
            val json = JSONObject(rawJson)
            val resultObj = json.optJSONObject("result")
            val resultText = if (resultObj != null) {
                resultObj.optNullableString("text")
            } else {
                null
            }

            return PythonResultPayload(
                requestId = json.optNullableString("requestId") ?: fallbackRequestId,
                status = json.optNullableString("status") ?: "error",
                errorCode = json.optNullableString("errorCode"),
                message = json.optNullableString("message") ?: "",
                resultText = resultText,
                stdout = json.optNullableString("stdout") ?: "",
                stderr = json.optNullableString("stderr") ?: "",
                error = json.optNullableString("error"),
                traceback = json.optNullableString("traceback"),
                isColdStart = isColdStart
            )
        }

        private fun JSONObject.optNullableString(key: String): String? {
            if (!has(key) || isNull(key)) {
                return null
            }

            val raw = opt(key)?.toString()?.trim().orEmpty()
            if (raw.isEmpty() || raw.equals("null", ignoreCase = true)) {
                return null
            }

            return raw
        }

        fun fromThrowable(error: Throwable, requestId: String, isColdStart: Boolean): PythonResultPayload {
            return PythonResultPayload(
                requestId = requestId,
                status = "error",
                errorCode = "PYTHON_RUNTIME_ERROR",
                message = error.message ?: error::class.java.simpleName,
                resultText = null,
                stdout = "",
                stderr = "",
                error = error.message ?: error::class.java.simpleName,
                traceback = error.stackTraceToString(),
                isColdStart = isColdStart
            )
        }

        fun timeout(requestId: String, timeoutMs: Long, isColdStart: Boolean): PythonResultPayload {
            return PythonResultPayload(
                requestId = requestId,
                status = "error",
                errorCode = "PYTHON_EXEC_TIMEOUT",
                message = "Python execution timed out after ${timeoutMs}ms",
                resultText = null,
                stdout = "",
                stderr = "",
                error = "timeout",
                traceback = null,
                isColdStart = isColdStart
            )
        }

        fun cancelled(requestId: String, reason: String, isColdStart: Boolean): PythonResultPayload {
            return PythonResultPayload(
                requestId = requestId,
                status = "cancelled",
                errorCode = "PYTHON_CANCELLED",
                message = reason,
                resultText = null,
                stdout = "",
                stderr = "",
                error = reason,
                traceback = null,
                isColdStart = isColdStart
            )
        }

        fun inputError(requestId: String, errorCode: String, message: String): PythonResultPayload {
            return PythonResultPayload(
                requestId = requestId,
                status = "error",
                errorCode = errorCode,
                message = message,
                resultText = null,
                stdout = "",
                stderr = "",
                error = message,
                traceback = null,
                isColdStart = false
            )
        }
    }
}
