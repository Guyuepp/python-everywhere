package com.example.pythonspike

import android.content.Context
import androidx.test.core.app.ActivityScenario
import androidx.test.core.app.ApplicationProvider
import androidx.test.ext.junit.runners.AndroidJUnit4
import androidx.test.espresso.Espresso.onView
import androidx.test.espresso.action.ViewActions.click
import androidx.test.espresso.assertion.ViewAssertions.matches
import androidx.test.espresso.matcher.ViewMatchers.isDisplayed
import androidx.test.espresso.matcher.ViewMatchers.withId
import androidx.test.espresso.matcher.ViewMatchers.withText
import org.hamcrest.Matchers.containsString
import org.junit.After
import org.junit.Before
import org.junit.Test
import org.junit.runner.RunWith

@RunWith(AndroidJUnit4::class)
class HistoryActivityTest {
    private lateinit var appContext: Context

    @Before
    fun setUp() {
        appContext = ApplicationProvider.getApplicationContext()
        ExecutionHistoryRepository.clearAll(appContext)
    }

    @After
    fun tearDown() {
        ExecutionHistoryRepository.clearAll(appContext)
    }

    @Test
    fun historyScreen_showsPersistedRecord() {
        val payload = ResultPayload(
            protocol = "process_text_result",
            protocolVersion = "1.0.0",
            requestId = "req-history-1",
            status = "success",
            errorCode = null,
            message = "ok",
            result = mapOf("text" to "7"),
            stdout = "",
            stderr = "",
            traceback = null,
            isColdStart = false,
            durationMs = 12L
        )

        ExecutionHistoryRepository.append(
            context = appContext,
            payload = payload,
            inputPreview = "history-input",
            source = "PROCESS_TEXT",
            createdAtEpochMs = 1_700_000_000_000L
        )

        val scenario = ActivityScenario.launch(HistoryActivity::class.java)
        try {
            onView(withText(containsString("SUCCESS"))).check(matches(isDisplayed()))
            onView(withText(containsString("history-input"))).check(matches(isDisplayed()))
        } finally {
            scenario.close()
        }
    }

    @Test
    fun clearHistory_clearsAllRecords() {
        val payload = ResultPayload(
            protocol = "process_text_result",
            protocolVersion = "1.0.0",
            requestId = "req-history-2",
            status = "error",
            errorCode = "PYTHON_RUNTIME_ERROR",
            message = "boom",
            result = null,
            stdout = "",
            stderr = "",
            traceback = null,
            isColdStart = false,
            durationMs = 20L
        )

        ExecutionHistoryRepository.append(
            context = appContext,
            payload = payload,
            inputPreview = "to-clear",
            source = "PROCESS_TEXT",
            createdAtEpochMs = 1_700_000_000_010L
        )

        val scenario = ActivityScenario.launch(HistoryActivity::class.java)
        try {
            onView(withId(R.id.historyClearButton)).perform(click())
            onView(withText(R.string.history_clear_confirm)).perform(click())

            onView(withId(R.id.historyEmptyText))
                .check(matches(withText(containsString("No history yet"))))
        } finally {
            scenario.close()
        }
    }
}
