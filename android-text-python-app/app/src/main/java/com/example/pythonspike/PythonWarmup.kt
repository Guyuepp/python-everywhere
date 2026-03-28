package com.example.pythonspike

import android.app.Application
import android.content.Context
import com.chaquo.python.Python
import com.chaquo.python.android.AndroidPlatform
import java.util.concurrent.Executors
import java.util.concurrent.TimeUnit
import java.util.concurrent.TimeoutException
import java.util.concurrent.atomic.AtomicBoolean

object PythonWarmupState {
    @Volatile
    var prewarmAttempted: Boolean = false

    @Volatile
    var prewarmSuccess: Boolean = false

    @Volatile
    var prewarmTimedOut: Boolean = false

    @Volatile
    var prewarmStartedAtMs: Long = 0L

    @Volatile
    var prewarmFinishedAtMs: Long = 0L
}

object PythonRuntime {
    private val startLock = Any()

    fun ensureStarted(context: Context) {
        if (Python.isStarted()) {
            return
        }

        synchronized(startLock) {
            if (!Python.isStarted()) {
                Python.start(AndroidPlatform(context.applicationContext))
            }
        }
    }
}

object PythonPrewarmer {
    private const val SCOPE = "PythonPrewarm"
    private const val PREWARM_TIMEOUT_MS = 2500L
    private val scheduled = AtomicBoolean(false)

    fun schedule(application: Application) {
        if (!scheduled.compareAndSet(false, true)) {
            return
        }

        val executor = Executors.newSingleThreadExecutor { runnable ->
            Thread(runnable, "python-prewarm-worker").apply { isDaemon = true }
        }

        PythonWarmupState.prewarmAttempted = true
        PythonWarmupState.prewarmStartedAtMs = System.currentTimeMillis()
        AppLog.d(SCOPE, "background prewarm started")

        val prewarmFuture = executor.submit<Boolean> {
            runCatching {
                PythonRuntime.ensureStarted(application)
                true
            }.getOrElse { error ->
                AppLog.w(SCOPE, "prewarm failed", error)
                false
            }
        }

        Thread(
            {
                try {
                    val ok = prewarmFuture.get(PREWARM_TIMEOUT_MS, TimeUnit.MILLISECONDS)
                    PythonWarmupState.prewarmSuccess = ok && Python.isStarted()
                    PythonWarmupState.prewarmFinishedAtMs = System.currentTimeMillis()
                    AppLog.d(SCOPE, "background prewarm completed success=${PythonWarmupState.prewarmSuccess}")
                } catch (_: TimeoutException) {
                    PythonWarmupState.prewarmTimedOut = true
                    PythonWarmupState.prewarmFinishedAtMs = System.currentTimeMillis()
                    AppLog.w(SCOPE, "background prewarm timeout, abandon waiting")
                } catch (error: Exception) {
                    PythonWarmupState.prewarmFinishedAtMs = System.currentTimeMillis()
                    AppLog.w(SCOPE, "background prewarm unexpected error", error)
                } finally {
                    // Do not force-stop worker thread; just stop accepting new tasks.
                    executor.shutdown()
                }
            },
            "python-prewarm-watchdog"
        ).start()
    }
}
