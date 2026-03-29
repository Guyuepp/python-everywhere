package com.example.pythonspike

import android.content.ComponentName
import android.content.Context
import android.content.Intent
import androidx.test.core.app.ApplicationProvider
import androidx.test.ext.junit.runners.AndroidJUnit4
import org.junit.Assert.assertTrue
import org.junit.Test
import org.junit.runner.RunWith

@RunWith(AndroidJUnit4::class)
class LauncherEntryRoutingTest {
    @Test
    fun launcherIntent_resolvesToHistoryActivity() {
        val context = ApplicationProvider.getApplicationContext<Context>()
        val launcherIntent = Intent(Intent.ACTION_MAIN).apply {
            addCategory(Intent.CATEGORY_LAUNCHER)
            `package` = context.packageName
        }

        val resolveInfo = context.packageManager.resolveActivity(launcherIntent, 0)
        val resolvedClassName = resolveInfo?.activityInfo?.name.orEmpty()

        assertTrue(
            "Expected launcher to resolve to HistoryActivity, but got $resolvedClassName",
            resolvedClassName == HistoryActivity::class.java.name
        )
    }

    @Test
    fun processTextIntent_resolvesToMainActivity() {
        val context = ApplicationProvider.getApplicationContext<Context>()
        val processTextIntent = Intent(Intent.ACTION_PROCESS_TEXT).apply {
            addCategory(Intent.CATEGORY_DEFAULT)
            type = "text/plain"
            `package` = context.packageName
        }

        val candidates = context.packageManager.queryIntentActivities(processTextIntent, 0)
        val hasMainActivityHandler = candidates.any { info ->
            ComponentName(info.activityInfo.packageName, info.activityInfo.name).className ==
                MainActivity::class.java.name
        }

        assertTrue("Expected PROCESS_TEXT to be handled by MainActivity", hasMainActivityHandler)
    }
}
