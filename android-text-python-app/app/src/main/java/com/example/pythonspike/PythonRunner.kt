package com.example.pythonspike

import kotlinx.coroutines.CancellationException
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.delay
import kotlinx.coroutines.withContext
import java.util.concurrent.ExecutorService
import java.util.concurrent.Executors
import java.util.concurrent.TimeUnit
import java.util.concurrent.TimeoutException
import java.util.concurrent.atomic.AtomicReference

class PythonRunner(
    private val bridge: PythonBridge,
    private val requestManager: PythonRequestManager = PythonRequestManager(),
    private val executionExecutor: ExecutorService = Executors.newSingleThreadExecutor { runnable ->
        Thread(runnable, "python-exec-worker")
    }
) {
    private val inFlightRequestId = AtomicReference<String?>(null)

    suspend fun processText(
        input: String,
        requestHandle: PythonRequestHandle,
        options: PythonExecutionOptions = PythonExecutionOptions()
    ): PythonResultPayload = withContext(Dispatchers.IO) {
        val requestId = requestHandle.requestId
        val isColdStart = !bridge.isStarted()
        val startedAtNs = System.nanoTime()

        val acquired = acquireExecutionGate(
            requestId = requestId,
            acquireTimeoutMs = options.gateAcquireTimeoutMs,
            retryIntervalMs = options.gateRetryIntervalMs
        )
        if (!acquired) {
            requestManager.complete(requestId)
            AppLog.w(
                "PythonRunner",
                "execution gate acquire timeout, reject requestId=$requestId busyWith=${inFlightRequestId.get()}"
            )
            return@withContext PythonResultPayload.cancelled(
                requestId = requestId,
                reason = "Execution gate acquire timeout",
                isColdStart = isColdStart
            )
        }

        AppLog.d(
            "PythonRunner",
            "start requestId=$requestId inputLength=${input.length} timeoutMs=${options.timeoutMs} isColdStart=$isColdStart"
        )

        val task = executionExecutor.submit<PythonResultPayload> {
            try {
                val payloadJson = bridge.processText(input, requestId, options)
                PythonResultPayload.fromJson(payloadJson, isColdStart, requestId)
            } catch (error: Throwable) {
                AppLog.w("PythonRunner", "processText failed requestId=$requestId", error)
                PythonResultPayload.fromThrowable(error, requestId, isColdStart)
            } finally {
                bridge.clearRequest(requestId)
                requestManager.complete(requestId)
                inFlightRequestId.compareAndSet(requestId, null)
            }
        }

        try {
            val result = task.get(options.timeoutMs, TimeUnit.MILLISECONDS)
            val durationMs = (System.nanoTime() - startedAtNs) / 1_000_000
            recordBaseline(
                requestId = requestId,
                isColdStart = isColdStart,
                durationMs = durationMs,
                status = result.status,
                errorCode = result.errorCode
            )
            AppLog.d(
                "PythonRunner",
                "completed requestId=$requestId status=${result.status} errorCode=${result.errorCode ?: "none"}"
            )
            result
        } catch (_: TimeoutException) {
            requestManager.cancel(requestId)
            bridge.cancelRequest(requestId)
            // Never hard-interrupt Python. If task hasn't started yet, this drops it from queue.
            task.cancel(false)
            AppLog.w("PythonRunner", "timeout requestId=$requestId, abandon waiting")
            // Intentionally do not hard-stop worker thread/interpreter.
            val timeoutPayload = PythonResultPayload.timeout(requestId, options.timeoutMs, isColdStart)
            val durationMs = (System.nanoTime() - startedAtNs) / 1_000_000
            recordBaseline(
                requestId = requestId,
                isColdStart = isColdStart,
                durationMs = durationMs,
                status = timeoutPayload.status,
                errorCode = timeoutPayload.errorCode
            )
            timeoutPayload
        } catch (_: CancellationException) {
            requestManager.cancel(requestId)
            bridge.cancelRequest(requestId)
            task.cancel(false)
            AppLog.d("PythonRunner", "cancelled requestId=$requestId by coroutine context")
            val cancelledPayload = PythonResultPayload.cancelled(requestId, "Cancelled by lifecycle", isColdStart)
            val durationMs = (System.nanoTime() - startedAtNs) / 1_000_000
            recordBaseline(
                requestId = requestId,
                isColdStart = isColdStart,
                durationMs = durationMs,
                status = cancelledPayload.status,
                errorCode = cancelledPayload.errorCode
            )
            cancelledPayload
        } catch (error: InterruptedException) {
            Thread.currentThread().interrupt()
            requestManager.cancel(requestId)
            bridge.cancelRequest(requestId)
            task.cancel(false)
            AppLog.w("PythonRunner", "interrupted waiting requestId=$requestId", error)
            val interruptedPayload = PythonResultPayload.cancelled(requestId, "Interrupted while waiting", isColdStart)
            val durationMs = (System.nanoTime() - startedAtNs) / 1_000_000
            recordBaseline(
                requestId = requestId,
                isColdStart = isColdStart,
                durationMs = durationMs,
                status = interruptedPayload.status,
                errorCode = interruptedPayload.errorCode
            )
            interruptedPayload
        }
    }

    private fun recordBaseline(
        requestId: String,
        isColdStart: Boolean,
        durationMs: Long,
        status: String,
        errorCode: String?
    ) {
        PerformanceBaselineTracker.record(isColdStart = isColdStart, durationMs = durationMs)
        val snapshot = PerformanceBaselineTracker.snapshot()
        AppLog.d(
            "PerfBaseline",
            "requestId=$requestId status=$status errorCode=${errorCode ?: "none"} durationMs=$durationMs " +
                "coldCount=${snapshot.coldCount} warmCount=${snapshot.warmCount} avgMs=${snapshot.avgDurationMs} " +
                "coldAvgMs=${snapshot.coldAvgDurationMs} warmAvgMs=${snapshot.warmAvgDurationMs}"
        )
    }

    fun createRequest(): PythonRequestHandle = requestManager.createRequest()

    fun cancelActiveRequest(reason: String): Boolean {
        val active = requestManager.cancelActive() ?: return false
        bridge.cancelRequest(active.requestId)
        AppLog.d("PythonRunner", "cancel active requestId=${active.requestId} reason=$reason")
        return true
    }

    fun cancelRequest(requestId: String, reason: String): Boolean {
        val cancelled = requestManager.cancel(requestId)
        if (cancelled) {
            bridge.cancelRequest(requestId)
            AppLog.d("PythonRunner", "cancel requestId=$requestId reason=$reason")
        }
        return cancelled
    }

    private suspend fun acquireExecutionGate(
        requestId: String,
        acquireTimeoutMs: Long,
        retryIntervalMs: Long
    ): Boolean {
        if (inFlightRequestId.compareAndSet(null, requestId)) {
            return true
        }

        val timeoutNs = acquireTimeoutMs.coerceAtLeast(0L) * 1_000_000L
        val deadlineNs = System.nanoTime() + timeoutNs
        val sleepMs = retryIntervalMs.coerceAtLeast(5L)

        while (System.nanoTime() < deadlineNs) {
            if (requestManager.isCancelled(requestId)) {
                return false
            }

            delay(sleepMs)
            if (inFlightRequestId.compareAndSet(null, requestId)) {
                return true
            }
        }

        return false
    }
}
