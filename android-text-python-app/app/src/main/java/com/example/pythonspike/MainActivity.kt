package com.example.pythonspike

import android.content.ClipData
import android.content.ClipboardManager
import android.content.Context
import android.content.Intent
import android.os.TransactionTooLargeException
import android.os.Bundle
import android.view.View
import android.widget.Button
import android.widget.ProgressBar
import android.widget.TextView
import android.widget.Toast
import androidx.appcompat.app.AppCompatActivity
import androidx.lifecycle.lifecycleScope
import kotlinx.coroutines.CancellationException
import kotlinx.coroutines.Job
import kotlinx.coroutines.launch
import org.json.JSONObject

class MainActivity : AppCompatActivity() {
    private val pythonRunner by lazy {
        PythonRunner(
            bridge = ChaquopyPythonBridge(applicationContext)
        )
    }
    private lateinit var statusText: TextView
    private lateinit var durationText: TextView
    private lateinit var messageText: TextView
    private lateinit var progressBar: ProgressBar
    private lateinit var copyButton: Button
    private lateinit var closeButton: Button

    private lateinit var stdoutHeader: TextView
    private lateinit var stdoutContainer: View
    private lateinit var stdoutText: TextView

    private lateinit var stderrHeader: TextView
    private lateinit var stderrContainer: View
    private lateinit var stderrText: TextView

    private lateinit var tracebackHeader: TextView
    private lateinit var tracebackContainer: View
    private lateinit var tracebackText: TextView

    private var activeJob: Job? = null
    private var activeRequest: PythonRequestHandle? = null
    private var lastResultJson: String? = null

    private data class ParsedExecutionInput(
        val input: String,
        val debugDelayMs: Long,
        val debugFail: Boolean,
        val gateAcquireTimeoutMs: Long
    )

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)

        setContentView(R.layout.activity_result_dialog)
        bindViews()
        copyButton.setOnClickListener { copyResultJsonToClipboard() }
        closeButton.setOnClickListener { finish() }
        setupExpandableHeader(stdoutHeader, stdoutContainer, getString(R.string.result_stdout_title))
        setupExpandableHeader(stderrHeader, stderrContainer, getString(R.string.result_stderr_title))
        setupExpandableHeader(tracebackHeader, tracebackContainer, getString(R.string.result_traceback_title))

        renderRunningState()

        AppLog.d(
            "MainActivity",
            "first frame rendered, prewarmAttempted=${PythonWarmupState.prewarmAttempted}, " +
                "prewarmSuccess=${PythonWarmupState.prewarmSuccess}, prewarmTimedOut=${PythonWarmupState.prewarmTimedOut}"
        )

        startExecutionFromIntent(intent, source = "onCreate")
    }

    override fun onNewIntent(newIntent: Intent) {
        super.onNewIntent(newIntent)
        setIntent(newIntent)
        startExecutionFromIntent(newIntent, source = "onNewIntent")
    }

    private fun startExecutionFromIntent(srcIntent: Intent?, source: String) {
        activeJob?.cancel(CancellationException("replaced_by_new_intent"))
        cancelActiveRequest("replace_$source")

        val parsed = parseExecutionInput(srcIntent, source) ?: return
        val request = pythonRunner.createRequest()
        activeRequest = request
        renderRunningState()
        val startedAtNs = System.nanoTime()

        AppLog.d(
            "MainActivity",
            "execute source=$source action=${srcIntent?.action ?: "none"} requestId=${request.requestId} " +
                "inputLength=${parsed.input.length}"
        )

        activeJob = lifecycleScope.launch {
            try {
                val payload = pythonRunner.processText(
                    input = parsed.input,
                    requestHandle = request,
                    options = PythonExecutionOptions(
                        timeoutMs = 5_000L,
                        gateAcquireTimeoutMs = parsed.gateAcquireTimeoutMs,
                        debugDelayMs = parsed.debugDelayMs,
                        debugFail = parsed.debugFail
                    )
                )

                val durationMs = (System.nanoTime() - startedAtNs) / 1_000_000
                renderPayload(payload, durationMs)
                AppLog.d(
                    "MainActivity",
                    "rendered requestId=${payload.requestId} status=${payload.status} " +
                        "errorCode=${payload.errorCode ?: "none"} isColdStart=${payload.isColdStart} " +
                        "tracebackPresent=${!payload.traceback.isNullOrBlank()}"
                )
            } catch (_: CancellationException) {
                AppLog.d("MainActivity", "python job cancelled by lifecycle")
            } finally {
                if (activeRequest?.requestId == request.requestId) {
                    activeRequest = null
                }
            }
        }
    }

    private fun parseExecutionInput(srcIntent: Intent?, source: String): ParsedExecutionInput? {
        val gateAcquireTimeoutMs = if (source == "onNewIntent") 2_000L else 800L
        val readOutcome = ProcessTextIntentReader.read(
            action = srcIntent?.action,
            readProcessText = {
                srcIntent?.getCharSequenceExtra(Intent.EXTRA_PROCESS_TEXT)
            },
            readDebugDelayMs = {
                srcIntent?.getLongExtra("debug_delay_ms", 0L) ?: 0L
            },
            readDebugFail = {
                srcIntent?.getBooleanExtra("debug_fail", false) ?: false
            },
            isIntentTooLargeError = ::isIntentTooLargeError
        )

        if (readOutcome.errorCode != null) {
            AppLog.w(
                "MainActivity",
                "input read failed source=$source code=${readOutcome.errorCode}"
            )
            renderPayload(
                PythonResultPayload.inputError(
                    requestId = "input-${System.nanoTime()}",
                    errorCode = readOutcome.errorCode,
                    message = readOutcome.message ?: "Failed to read input"
                ),
                durationMs = 0L
            )
            return null
        }

        val validation = InputValidator.validate(readOutcome.input)
        if (!validation.isValid) {
            renderPayload(
                PythonResultPayload.inputError(
                    requestId = "input-${System.nanoTime()}",
                    errorCode = validation.errorCode ?: "INVALID_INPUT",
                    message = validation.message
                ),
                durationMs = 0L
            )
            return null
        }

        return ParsedExecutionInput(
            input = validation.normalizedText,
            debugDelayMs = readOutcome.debugDelayMs,
            debugFail = readOutcome.debugFail,
            gateAcquireTimeoutMs = gateAcquireTimeoutMs
        )
    }

    fun dispatchNewIntentForTest(newIntent: Intent) {
        onNewIntent(newIntent)
    }

    private fun bindViews() {
        statusText = findViewById(R.id.statusText)
        durationText = findViewById(R.id.durationText)
        messageText = findViewById(R.id.messageText)
        progressBar = findViewById(R.id.progressBar)
        copyButton = findViewById(R.id.copyButton)
        closeButton = findViewById(R.id.closeButton)

        stdoutHeader = findViewById(R.id.stdoutHeader)
        stdoutContainer = findViewById(R.id.stdoutContainer)
        stdoutText = findViewById(R.id.stdoutText)

        stderrHeader = findViewById(R.id.stderrHeader)
        stderrContainer = findViewById(R.id.stderrContainer)
        stderrText = findViewById(R.id.stderrText)

        tracebackHeader = findViewById(R.id.tracebackHeader)
        tracebackContainer = findViewById(R.id.tracebackContainer)
        tracebackText = findViewById(R.id.tracebackText)
    }

    private fun renderRunningState() {
        statusText.text = getString(R.string.result_status_running)
        statusText.setTextColor(0xFF0E7A0D.toInt())
        durationText.text = getString(R.string.result_duration_running)
        messageText.text = getString(R.string.result_message_running)
        progressBar.visibility = View.VISIBLE
        copyButton.isEnabled = false
        lastResultJson = null

        hideDetailSection(stdoutHeader, stdoutContainer, stdoutText)
        hideDetailSection(stderrHeader, stderrContainer, stderrText)
        hideDetailSection(tracebackHeader, tracebackContainer, tracebackText)
    }

    private fun renderPayload(payload: PythonResultPayload, durationMs: Long) {
        val resultPayload = ResultPayload.fromExecution(payload, durationMs)
        lastResultJson = resultPayload.toJsonString()

        val renderedText = if (payload.isSuccess) {
            payload.resultText ?: "python_ok"
        } else if (payload.isCancelled) {
            "${payload.errorCode ?: "PYTHON_CANCELLED"}: ${payload.message}"
        } else {
            "${payload.errorCode ?: "UNKNOWN"}: ${payload.message}"
        }

        progressBar.visibility = View.GONE
        copyButton.isEnabled = true
        durationText.text = getString(R.string.result_duration_format, durationMs)

        if (payload.isSuccess) {
            statusText.text = getString(R.string.result_status_success)
            statusText.setTextColor(0xFF0E7A0D.toInt())
        } else {
            statusText.text = getString(R.string.result_status_failure)
            statusText.setTextColor(0xFFB00020.toInt())
        }

        messageText.text = renderedText

        bindDetailSection(
            title = getString(R.string.result_stdout_title),
            header = stdoutHeader,
            container = stdoutContainer,
            contentView = stdoutText,
            content = payload.stdout
        )
        bindDetailSection(
            title = getString(R.string.result_stderr_title),
            header = stderrHeader,
            container = stderrContainer,
            contentView = stderrText,
            content = payload.stderr
        )
        bindDetailSection(
            title = getString(R.string.result_traceback_title),
            header = tracebackHeader,
            container = tracebackContainer,
            contentView = tracebackText,
            content = payload.traceback.orEmpty()
        )
    }

    private fun copyResultJsonToClipboard() {
        val json = lastResultJson
        if (json.isNullOrBlank()) {
            Toast.makeText(this, getString(R.string.result_copy_empty), Toast.LENGTH_SHORT).show()
            return
        }

        val copied = runCatching {
            // Defensive check: ensure we only copy valid JSON payloads.
            JSONObject(json)
            val clipboard = getSystemService(Context.CLIPBOARD_SERVICE) as ClipboardManager
            val clip = ClipData.newPlainText("result-payload", json)
            clipboard.setPrimaryClip(clip)
            true
        }.getOrElse {
            AppLog.w("MainActivity", "copy json failed", it)
            false
        }

        if (copied) {
            Toast.makeText(this, getString(R.string.result_copy_success), Toast.LENGTH_SHORT).show()
        } else {
            Toast.makeText(this, getString(R.string.result_copy_failure), Toast.LENGTH_SHORT).show()
        }
    }

    private fun setupExpandableHeader(header: TextView, container: View, title: String) {
        header.setOnClickListener {
            val expand = container.visibility != View.VISIBLE
            container.visibility = if (expand) View.VISIBLE else View.GONE
            header.text = if (expand) "$title ▲" else "$title ▼"
        }
    }

    private fun bindDetailSection(
        title: String,
        header: TextView,
        container: View,
        contentView: TextView,
        content: String
    ) {
        if (content.isBlank()) {
            hideDetailSection(header, container, contentView)
            return
        }

        header.visibility = View.VISIBLE
        header.text = "$title ▼"
        container.visibility = View.GONE
        contentView.text = content
    }

    private fun hideDetailSection(header: TextView, container: View, contentView: TextView) {
        header.visibility = View.GONE
        container.visibility = View.GONE
        contentView.text = ""
    }

    private fun isIntentTooLargeError(error: Throwable): Boolean {
        if (error is TransactionTooLargeException) {
            return true
        }

        var current: Throwable? = error
        while (current != null) {
            val name = current::class.java.name
            val message = current.message.orEmpty()
            if (name.contains("TransactionTooLargeException") ||
                message.contains("FAILED BINDER TRANSACTION", ignoreCase = true) ||
                message.contains("TransactionTooLargeException", ignoreCase = true)
            ) {
                return true
            }
            current = current.cause
        }
        return false
    }

    override fun onStop() {
        super.onStop()
        cancelActiveRequest("activity_stop")
    }

    override fun onDestroy() {
        cancelActiveRequest("activity_destroy")
        super.onDestroy()
    }

    private fun cancelActiveRequest(reason: String) {
        val request = activeRequest ?: return
        if (pythonRunner.cancelRequest(request.requestId, reason)) {
            AppLog.d("MainActivity", "cancel requested requestId=${request.requestId} reason=$reason")
        }
    }
}
