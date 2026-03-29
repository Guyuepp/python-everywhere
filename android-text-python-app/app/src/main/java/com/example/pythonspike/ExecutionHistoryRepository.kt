package com.example.pythonspike

import android.content.Context
import org.json.JSONArray
import org.json.JSONObject

data class ExecutionHistoryItem(
    val createdAtEpochMs: Long,
    val requestId: String,
    val status: String,
    val errorCode: String?,
    val message: String,
    val inputPreview: String,
    val resultPreview: String,
    val durationMs: Long,
    val source: String
) {
    fun toJsonObject(): JSONObject {
        return JSONObject().apply {
            put("createdAtEpochMs", createdAtEpochMs)
            put("requestId", requestId)
            put("status", status)
            put("errorCode", errorCode ?: JSONObject.NULL)
            put("message", message)
            put("inputPreview", inputPreview)
            put("resultPreview", resultPreview)
            put("durationMs", durationMs)
            put("source", source)
        }
    }

    companion object {
        fun fromJsonObject(json: JSONObject): ExecutionHistoryItem {
            return ExecutionHistoryItem(
                createdAtEpochMs = json.optLong("createdAtEpochMs", 0L),
                requestId = json.optString("requestId", ""),
                status = json.optString("status", "error").ifBlank { "error" },
                errorCode = json.optNullableString("errorCode"),
                message = json.optString("message", ""),
                inputPreview = json.optString("inputPreview", ""),
                resultPreview = json.optString("resultPreview", ""),
                durationMs = json.optLong("durationMs", 0L).coerceAtLeast(0L),
                source = json.optString("source", "PROCESS_TEXT").ifBlank { "PROCESS_TEXT" }
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
    }
}

object ExecutionHistoryRepository {
    private const val PREFS_NAME = "execution_history"
    private const val KEY_ITEMS_JSON = "items_json"
    private val lock = Any()

    const val DEFAULT_MAX_ITEMS: Int = 100

    fun append(
        context: Context,
        payload: ResultPayload,
        inputPreview: String,
        source: String,
        createdAtEpochMs: Long = System.currentTimeMillis(),
        maxItems: Int = DEFAULT_MAX_ITEMS
    ) {
        synchronized(lock) {
            val prefs = context.getSharedPreferences(PREFS_NAME, Context.MODE_PRIVATE)
            val items = decodeItems(prefs.getString(KEY_ITEMS_JSON, "").orEmpty()).toMutableList()
            items.add(
                ExecutionHistoryItem(
                    createdAtEpochMs = createdAtEpochMs,
                    requestId = payload.requestId,
                    status = payload.status,
                    errorCode = payload.errorCode,
                    message = payload.message,
                    inputPreview = normalizePreview(inputPreview),
                    resultPreview = toResultPreview(payload.result),
                    durationMs = payload.durationMs,
                    source = source
                )
            )

            val trimmed = trimToLimit(items, maxItems)
            prefs.edit().putString(KEY_ITEMS_JSON, encodeItems(trimmed)).apply()
        }
    }

    fun listRecent(context: Context, limit: Int = DEFAULT_MAX_ITEMS): List<ExecutionHistoryItem> {
        synchronized(lock) {
            val prefs = context.getSharedPreferences(PREFS_NAME, Context.MODE_PRIVATE)
            val allItems = decodeItems(prefs.getString(KEY_ITEMS_JSON, "").orEmpty())
            return trimToLimit(allItems, limit)
        }
    }

    fun clearAll(context: Context) {
        synchronized(lock) {
            val prefs = context.getSharedPreferences(PREFS_NAME, Context.MODE_PRIVATE)
            prefs.edit().remove(KEY_ITEMS_JSON).apply()
        }
    }

    internal fun trimToLimit(items: List<ExecutionHistoryItem>, limit: Int): List<ExecutionHistoryItem> {
        if (limit <= 0) {
            return emptyList()
        }

        return items
            .sortedByDescending { it.createdAtEpochMs }
            .take(limit)
    }

    internal fun encodeItems(items: List<ExecutionHistoryItem>): String {
        val array = JSONArray()
        items.forEach { item ->
            array.put(item.toJsonObject())
        }
        return array.toString()
    }

    internal fun decodeItems(rawJson: String): List<ExecutionHistoryItem> {
        if (rawJson.isBlank()) {
            return emptyList()
        }

        return runCatching {
            val array = JSONArray(rawJson)
            buildList {
                for (index in 0 until array.length()) {
                    val json = array.optJSONObject(index) ?: continue
                    add(ExecutionHistoryItem.fromJsonObject(json))
                }
            }
        }.getOrElse {
            emptyList()
        }
    }

    private fun toResultPreview(result: Map<String, Any?>?): String {
        if (result.isNullOrEmpty()) {
            return ""
        }

        val rawValue = result["text"] ?: result.values.firstOrNull { it != null }
        return normalizePreview(rawValue?.toString().orEmpty())
    }

    private fun normalizePreview(value: String): String {
        if (value.isBlank()) {
            return ""
        }

        return value
            .replace("\n", " ")
            .replace("\r", " ")
            .trim()
            .take(200)
    }
}
