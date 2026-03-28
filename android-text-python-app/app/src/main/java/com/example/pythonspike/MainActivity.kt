package com.example.pythonspike

import android.os.Bundle
import android.widget.TextView
import androidx.appcompat.app.AppCompatActivity
import com.chaquo.python.Python
import com.chaquo.python.android.AndroidPlatform

class MainActivity : AppCompatActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)

        if (!Python.isStarted()) {
            Python.start(AndroidPlatform(this))
        }

        val result = runCatching {
            val python = Python.getInstance()
            val module = python.getModule("processor")
            module.callAttr("process_text", "hello").toString()
        }.getOrElse { error ->
            "python_error: ${error.message}"
        }

        setContentView(
            TextView(this).apply {
                text = result
                textSize = 20f
                setPadding(32, 32, 32, 32)
            }
        )
    }
}
