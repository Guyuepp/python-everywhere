package com.example.pythonspike

import org.json.JSONObject

data class ResultPayload(
    val protocol: String,
    val protocolVersion: String,
    val requestId: String,
    val status: String,
    val errorCode: String?,
    val message: String,
    val result: Map<String, Any?>?,
    val stdout: String,
    val stderr: String,
    val traceback: String?,
    val isColdStart: Boolean,
    val durationMs: Long
) {
    fun toJsonString(): String {
        return runCatching {
            val json = JSONObject()
            json.put("protocol", protocol)
            json.put("protocolVersion", protocolVersion)
            json.put("requestId", requestId)
            json.put("status", status)
            json.put("errorCode", errorCode ?: JSONObject.NULL)
            json.put("message", message)

            if (result == null) {
                json.put("result", JSONObject.NULL)
            } else {
                val resultObj = JSONObject()
                for ((key, value) in result) {
                    resultObj.put(key, value ?: JSONObject.NULL)
                }
                json.put("result", resultObj)
            }

            json.put("stdout", stdout)
            json.put("stderr", stderr)
            json.put("traceback", traceback ?: JSONObject.NULL)
            json.put("isColdStart", isColdStart)
            json.put("durationMs", durationMs)
            json.toString()
        }.getOrElse {
            // Always return valid JSON even when serialization path has an unexpected failure.
            JSONObject(
                mapOf(
                    "protocol" to "process_text_result",
                    "protocolVersion" to "1.0.0",
                    "requestId" to requestId,
                    "status" to "error",
                    "errorCode" to "PYTHON_RUNTIME_ERROR",
                    "message" to "Failed to serialize result payload",
                    "result" to JSONObject.NULL,
                    "stdout" to "",
                    "stderr" to "",
                    "traceback" to JSONObject.NULL,
                    "isColdStart" to isColdStart,
                    "durationMs" to durationMs
                )
            ).toString()
        }
    }

    companion object {
        fun fromExecution(payload: PythonResultPayload, durationMs: Long): ResultPayload {
            val resultObj = if (payload.resultText == null) {
                null
            } else {
                mapOf("text" to payload.resultText)
            }

            return ResultPayload(
                protocol = "process_text_result",
                protocolVersion = "1.0.0",
                requestId = payload.requestId,
                status = payload.status.ifBlank { "error" },
                errorCode = payload.errorCode,
                message = payload.message.ifBlank { payload.error ?: "" },
                result = resultObj,
                stdout = payload.stdout,
                stderr = payload.stderr,
                traceback = payload.traceback,
                isColdStart = payload.isColdStart,
                durationMs = durationMs.coerceAtLeast(0L)
            )
        }
    }
}
