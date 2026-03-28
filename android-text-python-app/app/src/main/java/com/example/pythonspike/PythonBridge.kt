package com.example.pythonspike

import android.content.Context
import com.chaquo.python.Python

data class PythonExecutionOptions(
    val timeoutMs: Long = 5_000L,
    val gateAcquireTimeoutMs: Long = 800L,
    val gateRetryIntervalMs: Long = 20L,
    val debugDelayMs: Long = 0L,
    val debugFail: Boolean = false
)

interface PythonBridge {
    fun isStarted(): Boolean
    fun processText(input: String, requestId: String, options: PythonExecutionOptions): String
    fun cancelRequest(requestId: String)
    fun clearRequest(requestId: String)
}

class ChaquopyPythonBridge(
    private val appContext: Context
) : PythonBridge {
    override fun isStarted(): Boolean = Python.isStarted()

    override fun processText(input: String, requestId: String, options: PythonExecutionOptions): String {
        PythonRuntime.ensureStarted(appContext)

        val python = Python.getInstance()
        val module = python.getModule("processor")
        val payload = module.callAttr(
            "process_text",
            input,
            requestId,
            options.debugDelayMs,
            options.debugFail
        )

        return python.getModule("json").callAttr("dumps", payload).toString()
    }

    override fun cancelRequest(requestId: String) {
        runCatching {
            if (Python.isStarted()) {
                Python.getInstance().getModule("processor").callAttr("cancel_request", requestId)
            }
        }.onFailure { error ->
            AppLog.w("PythonBridge", "cancel_request failed requestId=$requestId", error)
        }
    }

    override fun clearRequest(requestId: String) {
        runCatching {
            if (Python.isStarted()) {
                Python.getInstance().getModule("processor").callAttr("clear_request", requestId)
            }
        }.onFailure { error ->
            AppLog.w("PythonBridge", "clear_request failed requestId=$requestId", error)
        }
    }
}
