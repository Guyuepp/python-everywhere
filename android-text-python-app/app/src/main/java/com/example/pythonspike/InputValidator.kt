package com.example.pythonspike

data class InputValidationResult(
    val isValid: Boolean,
    val normalizedText: String,
    val errorCode: String? = null,
    val message: String = ""
)

object InputValidator {
    private const val MAX_INPUT_CHARS = 10_000

    fun validate(rawText: String?): InputValidationResult {
        if (rawText == null) {
            return invalid("Input is empty")
        }

        val normalized = rawText.trim()
        if (normalized.isEmpty()) {
            return invalid("Input is blank")
        }

        if (normalized.length > MAX_INPUT_CHARS) {
            return invalid("Input is too long (max $MAX_INPUT_CHARS chars)")
        }

        if (containsAbnormalContent(normalized)) {
            return invalid("Input contains abnormal content")
        }

        return InputValidationResult(
            isValid = true,
            normalizedText = normalized,
            message = "ok"
        )
    }

    private fun containsAbnormalContent(value: String): Boolean {
        if (value.contains('\uFFFD')) {
            return true
        }

        for (c in value) {
            if (c == '\u0000') {
                return true
            }

            if (c.isISOControl() && c != '\n' && c != '\r' && c != '\t') {
                return true
            }
        }

        return false
    }

    private fun invalid(message: String): InputValidationResult {
        return InputValidationResult(
            isValid = false,
            normalizedText = "",
            errorCode = "INVALID_INPUT",
            message = message
        )
    }
}
