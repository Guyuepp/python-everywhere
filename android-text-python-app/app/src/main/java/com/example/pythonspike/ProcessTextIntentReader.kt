package com.example.pythonspike

private const val PROCESS_TEXT_ACTION = "android.intent.action.PROCESS_TEXT"

data class IntentReadOutcome(
    val input: String,
    val debugDelayMs: Long,
    val debugFail: Boolean,
    val errorCode: String? = null,
    val message: String? = null
)

object ProcessTextIntentReader {
    fun read(
        action: String?,
        readProcessText: () -> CharSequence?,
        readDebugDelayMs: () -> Long,
        readDebugFail: () -> Boolean,
        isIntentTooLargeError: (Throwable) -> Boolean
    ): IntentReadOutcome {
        return try {
            val debugDelayMs = readDebugDelayMs().coerceAtLeast(0L)
            val debugFail = readDebugFail()
            val selectedText = if (action == PROCESS_TEXT_ACTION) {
                readProcessText()?.toString()
            } else {
                "'hello'"
            }

            IntentReadOutcome(
                input = selectedText.orEmpty(),
                debugDelayMs = debugDelayMs,
                debugFail = debugFail
            )
        } catch (error: Throwable) {
            val code = if (isIntentTooLargeError(error)) {
                "INTENT_TOO_LARGE"
            } else {
                "INVALID_INPUT"
            }
            val message = if (code == "INTENT_TOO_LARGE") {
                "Input is too large for intent transport"
            } else {
                "Failed to read input"
            }

            IntentReadOutcome(
                input = "",
                debugDelayMs = 0L,
                debugFail = false,
                errorCode = code,
                message = message
            )
        }
    }
}
