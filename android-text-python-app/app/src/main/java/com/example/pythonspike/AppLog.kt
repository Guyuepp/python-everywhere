package com.example.pythonspike

import android.util.Log

object AppLog {
    private const val BASE_TAG = "PythonSpike"

    private inline fun safeLog(block: () -> Unit) {
        runCatching { block() }
    }

    fun d(scope: String, message: String) {
        if (BuildConfig.DEBUG) {
            safeLog { Log.d("$BASE_TAG/$scope", message) }
        }
    }

    fun w(scope: String, message: String, error: Throwable? = null) {
        if (BuildConfig.DEBUG) {
            safeLog { Log.w("$BASE_TAG/$scope", message, error) }
        } else {
            safeLog { Log.w("$BASE_TAG/$scope", message) }
        }
    }
}
