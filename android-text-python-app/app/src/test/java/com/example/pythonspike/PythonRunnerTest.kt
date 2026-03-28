package com.example.pythonspike

import kotlinx.coroutines.async
import kotlinx.coroutines.delay
import kotlinx.coroutines.runBlocking
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Test
import java.util.concurrent.ConcurrentHashMap

class PythonRunnerTest {
    @Test
    fun processText_successPath() = runBlocking {
        val bridge = FakeBridge(mode = Mode.SUCCESS)
        val runner = PythonRunner(
            bridge = bridge,
            requestManager = PythonRequestManager { "req-success" }
        )

        val result = runner.processText(
            input = "hello",
            requestHandle = runner.createRequest(),
            options = PythonExecutionOptions(timeoutMs = 500)
        )

        assertEquals("success", result.status)
        assertEquals(null, result.errorCode)
        assertEquals("python_ok:hello", result.resultText)
        assertFalse(result.isCancelled)
    }

    @Test
    fun processText_exceptionPath() = runBlocking {
        val bridge = FakeBridge(mode = Mode.THROW)
        val runner = PythonRunner(
            bridge = bridge,
            requestManager = PythonRequestManager { "req-error" }
        )

        val result = runner.processText(
            input = "hello",
            requestHandle = runner.createRequest(),
            options = PythonExecutionOptions(timeoutMs = 500)
        )

        assertEquals("error", result.status)
        assertEquals("PYTHON_RUNTIME_ERROR", result.errorCode)
        assertTrue(result.message.isNotBlank())
    }

    @Test
    fun processText_cancelPath() = runBlocking {
        val bridge = FakeBridge(mode = Mode.DELAY_RESPECT_CANCEL, delayMs = 2_000)
        val runner = PythonRunner(
            bridge = bridge,
            requestManager = PythonRequestManager { "req-cancel" }
        )

        val request = runner.createRequest()
        val deferred = async {
            runner.processText(
                input = "hello",
                requestHandle = request,
                options = PythonExecutionOptions(timeoutMs = 5_000)
            )
        }

        delay(150)
        runner.cancelRequest(request.requestId, "test-cancel")

        val result = deferred.await()
        assertEquals("cancelled", result.status)
        assertEquals("PYTHON_CANCELLED", result.errorCode)
        assertTrue(result.isCancelled)
    }

    @Test
    fun processText_timeoutPath() = runBlocking {
        val bridge = FakeBridge(mode = Mode.DELAY_IGNORE_CANCEL, delayMs = 2_000)
        val runner = PythonRunner(
            bridge = bridge,
            requestManager = PythonRequestManager { "req-timeout" }
        )

        val result = runner.processText(
            input = "hello",
            requestHandle = runner.createRequest(),
            options = PythonExecutionOptions(timeoutMs = 200)
        )

        assertEquals("error", result.status)
        assertEquals("PYTHON_EXEC_TIMEOUT", result.errorCode)
        assertTrue(result.message.contains("timed out"))
    }

    @Test
    fun processText_repeatedTimeouts_convergesWithoutPileup() = runBlocking {
        val bridge = FakeBridge(mode = Mode.DELAY_IGNORE_CANCEL, delayMs = 350)
        val runner = PythonRunner(
            bridge = bridge,
            requestManager = PythonRequestManager {
                "req-timeout-loop-${System.nanoTime()}"
            }
        )

        repeat(5) {
            val result = runner.processText(
                input = "hello",
                requestHandle = runner.createRequest(),
                options = PythonExecutionOptions(timeoutMs = 100)
            )

            assertEquals("error", result.status)
            assertEquals("PYTHON_EXEC_TIMEOUT", result.errorCode)

            // Give the cooperative cleanup path time to release in-flight gate.
            delay(450)
        }
    }

    @Test
    fun processText_replaceRunningRequest_newRequestEventuallySucceeds() = runBlocking {
        val bridge = FakeBridge(mode = Mode.DELAY_RESPECT_CANCEL, delayMs = 1_200)
        val ids = ArrayDeque(listOf("req-old", "req-new"))
        val runner = PythonRunner(
            bridge = bridge,
            requestManager = PythonRequestManager {
                if (ids.isEmpty()) "req-fallback" else ids.removeFirst()
            }
        )

        val oldRequest = runner.createRequest()
        val oldDeferred = async {
            runner.processText(
                input = "old",
                requestHandle = oldRequest,
                options = PythonExecutionOptions(timeoutMs = 5_000, gateAcquireTimeoutMs = 100)
            )
        }

        delay(120)
        runner.cancelRequest(oldRequest.requestId, "replace")

        val newRequest = runner.createRequest()
        val newDeferred = async {
            runner.processText(
                input = "new",
                requestHandle = newRequest,
                options = PythonExecutionOptions(timeoutMs = 5_000, gateAcquireTimeoutMs = 2_000)
            )
        }

        val oldResult = oldDeferred.await()
        val newResult = newDeferred.await()

        assertEquals("cancelled", oldResult.status)
        assertEquals("PYTHON_CANCELLED", oldResult.errorCode)
        assertEquals("success", newResult.status)
        assertEquals("python_ok:new", newResult.resultText)
    }

    @Test
    fun processText_serialGate_secondShortAcquireTimeoutCancelled() = runBlocking {
        val bridge = FakeBridge(mode = Mode.DELAY_IGNORE_CANCEL, delayMs = 800)
        val ids = ArrayDeque(listOf("req-serial-old", "req-serial-new"))
        val runner = PythonRunner(
            bridge = bridge,
            requestManager = PythonRequestManager {
                if (ids.isEmpty()) "req-serial-fallback" else ids.removeFirst()
            }
        )

        val oldRequest = runner.createRequest()
        val oldDeferred = async {
            runner.processText(
                input = "old",
                requestHandle = oldRequest,
                options = PythonExecutionOptions(timeoutMs = 5_000, gateAcquireTimeoutMs = 800)
            )
        }

        delay(60)

        val newRequest = runner.createRequest()
        val newResult = runner.processText(
            input = "new",
            requestHandle = newRequest,
            options = PythonExecutionOptions(timeoutMs = 5_000, gateAcquireTimeoutMs = 120)
        )

        assertEquals("cancelled", newResult.status)
        assertEquals("PYTHON_CANCELLED", newResult.errorCode)

        val oldResult = oldDeferred.await()
        assertEquals("success", oldResult.status)
        assertEquals("python_ok:old", oldResult.resultText)
    }
}

private enum class Mode {
    SUCCESS,
    THROW,
    DELAY_RESPECT_CANCEL,
    DELAY_IGNORE_CANCEL
}

private class FakeBridge(
    private val mode: Mode,
    private val delayMs: Long = 0L
) : PythonBridge {
    private val cancelledRequests = ConcurrentHashMap.newKeySet<String>()

    override fun isStarted(): Boolean = true

    override fun processText(input: String, requestId: String, options: PythonExecutionOptions): String {
        return when (mode) {
            Mode.SUCCESS -> successJson(requestId, input)
            Mode.THROW -> throw IllegalStateException("fake bridge failure")
            Mode.DELAY_RESPECT_CANCEL -> delayedJson(requestId, input, respectCancel = true)
            Mode.DELAY_IGNORE_CANCEL -> delayedJson(requestId, input, respectCancel = false)
        }
    }

    override fun cancelRequest(requestId: String) {
        cancelledRequests.add(requestId)
    }

    override fun clearRequest(requestId: String) {
        cancelledRequests.remove(requestId)
    }

    private fun delayedJson(requestId: String, input: String, respectCancel: Boolean): String {
        var elapsed = 0L
        while (elapsed < delayMs) {
            Thread.sleep(50)
            elapsed += 50

            if (respectCancel && cancelledRequests.contains(requestId)) {
                return """
                    {
                                            "requestId": "$requestId",
                                            "status": "cancelled",
                                            "errorCode": "PYTHON_CANCELLED",
                                            "message": "Cancelled by host token",
                                            "result": null,
                                            "stdout": "",
                                            "stderr": "",
                                            "error": "Cancelled by host token",
                                            "traceback": null
                    }
                """.trimIndent()
            }
        }

        return successJson(requestId, input)
    }

    private fun successJson(requestId: String, input: String): String {
        return """
            {
              "requestId": "$requestId",
              "status": "success",
              "errorCode": null,
              "message": "ok",
              "result": {"text": "python_ok:$input"},
              "stdout": "",
              "stderr": "",
              "error": null,
              "traceback": null
            }
        """.trimIndent()
    }
}
