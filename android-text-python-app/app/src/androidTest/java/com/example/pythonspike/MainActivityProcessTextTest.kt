package com.example.pythonspike

import android.content.ClipboardManager
import android.content.Context
import android.content.Intent
import android.widget.TextView
import androidx.test.core.app.ActivityScenario
import androidx.test.core.app.ApplicationProvider
import androidx.test.ext.junit.runners.AndroidJUnit4
import androidx.test.espresso.Espresso.onView
import androidx.test.espresso.action.ViewActions.click
import androidx.test.espresso.matcher.ViewMatchers.withId
import org.json.JSONObject
import org.junit.Assert.assertEquals
import org.junit.Assert.assertNotNull
import org.junit.Assert.assertTrue
import org.junit.Test
import org.junit.runner.RunWith

@RunWith(AndroidJUnit4::class)
class MainActivityProcessTextTest {
    @Test
    fun processTextLaunch_copyJson_flowPasses() {
        val launchIntent = Intent(Intent.ACTION_PROCESS_TEXT).apply {
            setClassName(
                ApplicationProvider.getApplicationContext<Context>(),
                MainActivity::class.java.name
            )
            putExtra(Intent.EXTRA_PROCESS_TEXT, "'copy-flow'")
        }

        val scenario = ActivityScenario.launch<MainActivity>(launchIntent)
        try {
            waitUntil(scenario) { activity ->
                activity.findViewById<TextView>(R.id.statusText).text.toString() == "Success"
            }

            onView(withId(R.id.copyButton)).perform(click())

            val context = ApplicationProvider.getApplicationContext<Context>()
            val clipboard = context.getSystemService(Context.CLIPBOARD_SERVICE) as ClipboardManager
            val copied = clipboard.primaryClip?.getItemAt(0)?.coerceToText(context)?.toString().orEmpty()
            val copiedJson = JSONObject(copied)

            assertEquals("process_text_result", copiedJson.getString("protocol"))
            assertEquals("success", copiedJson.getString("status"))
            assertTrue(copiedJson.getJSONObject("result").getString("text").contains("copy-flow"))
        } finally {
            closeScenarioSafely(scenario)
        }
    }

    @Test
    fun onNewIntent_latestRequestWins() {
        val context = ApplicationProvider.getApplicationContext<Context>()
        val firstIntent = Intent(Intent.ACTION_PROCESS_TEXT).apply {
            setClassName(context, MainActivity::class.java.name)
            putExtra(Intent.EXTRA_PROCESS_TEXT, "'old'")
            putExtra("debug_delay_ms", 1200L)
        }

        val scenario = ActivityScenario.launch<MainActivity>(firstIntent)
        try {
            Thread.sleep(120)

            val nextIntent = Intent(Intent.ACTION_PROCESS_TEXT).apply {
                putExtra(Intent.EXTRA_PROCESS_TEXT, "'new'")
                putExtra("debug_delay_ms", 0L)
            }
            scenario.onActivity { activity ->
                activity.dispatchNewIntentForTest(nextIntent)
            }

            waitUntil(scenario) { activity ->
                activity.findViewById<TextView>(R.id.messageText).text.toString().contains("new")
            }
        } finally {
            closeScenarioSafely(scenario)
        }
    }

    @Test
    fun processTextLaunch_persistsHistoryRecord() {
        val context = ApplicationProvider.getApplicationContext<Context>()
        ExecutionHistoryRepository.clearAll(context)

        val launchIntent = Intent(Intent.ACTION_PROCESS_TEXT).apply {
            setClassName(context, MainActivity::class.java.name)
            putExtra(Intent.EXTRA_PROCESS_TEXT, "'history-flow'")
        }

        val scenario = ActivityScenario.launch<MainActivity>(launchIntent)
        try {
            waitUntil(scenario) { activity ->
                activity.findViewById<TextView>(R.id.statusText).text.toString() == "Success"
            }

            val historyItem = waitUntilHistoryRecord(
                context = context,
                timeoutMs = 10_000L
            ) { item ->
                item.inputPreview.contains("history-flow")
            }

            assertNotNull("Expected a history record for Process Text execution", historyItem)
            assertEquals("PROCESS_TEXT", historyItem?.source)
            assertEquals("success", historyItem?.status)
            assertTrue((historyItem?.resultPreview ?: "").contains("history-flow"))
            assertTrue((historyItem?.durationMs ?: 0L) >= 0L)
        } finally {
            closeScenarioSafely(scenario)
        }
    }

    private fun closeScenarioSafely(scenario: ActivityScenario<MainActivity>) {
        runCatching {
            scenario.onActivity { activity ->
                activity.finish()
            }
            Thread.sleep(150)
            scenario.close()
        }.onFailure {
            // Some devices keep translucent singleTop activities in RESUMED briefly.
            // Teardown failures should not mask assertions already validated in the test body.
        }
    }

    private fun waitUntil(
        scenario: ActivityScenario<MainActivity>,
        timeoutMs: Long = 10_000L,
        condition: (MainActivity) -> Boolean
    ) {
        val deadline = System.currentTimeMillis() + timeoutMs
        while (System.currentTimeMillis() < deadline) {
            var satisfied = false
            scenario.onActivity { activity ->
                satisfied = condition(activity)
            }
            if (satisfied) {
                return
            }
            Thread.sleep(100)
        }
        throw AssertionError("Timed out waiting for UI condition")
    }

    private fun waitUntilHistoryRecord(
        context: Context,
        timeoutMs: Long,
        predicate: (ExecutionHistoryItem) -> Boolean
    ): ExecutionHistoryItem? {
        val deadline = System.currentTimeMillis() + timeoutMs
        while (System.currentTimeMillis() < deadline) {
            val match = ExecutionHistoryRepository
                .listRecent(context, limit = 100)
                .firstOrNull(predicate)
            if (match != null) {
                return match
            }
            Thread.sleep(100)
        }
        return null
    }
}
