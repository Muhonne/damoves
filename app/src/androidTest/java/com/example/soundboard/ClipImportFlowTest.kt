package com.example.soundboard

import android.content.Context
import android.content.Intent
import android.net.Uri
import android.widget.TextView
import androidx.test.core.app.ActivityScenario
import androidx.test.core.app.ApplicationProvider
import androidx.test.espresso.Espresso.onView
import androidx.test.espresso.action.ViewActions.click
import androidx.test.espresso.action.ViewActions.replaceText
import androidx.test.espresso.assertion.ViewAssertions.matches
import androidx.test.espresso.matcher.ViewMatchers.isDisplayed
import androidx.test.espresso.matcher.ViewMatchers.withId
import androidx.test.espresso.matcher.ViewMatchers.withText
import androidx.test.ext.junit.runners.AndroidJUnit4
import androidx.test.filters.LargeTest
import androidx.test.platform.app.InstrumentationRegistry
import org.junit.After
import org.junit.Assert.assertTrue
import org.junit.Test
import org.junit.runner.RunWith
import java.io.File

@RunWith(AndroidJUnit4::class)
@LargeTest
class ClipImportFlowTest {

    private val context: Context
        get() = InstrumentationRegistry.getInstrumentation().targetContext
    private val moveName = "Flow test clip"

    @After
    fun removeSavedMove() {
        val preferences = context.getSharedPreferences("imports", Context.MODE_PRIVATE)
        preferences.all
            .filterValues { it == moveName }
            .forEach { (id, _) ->
                File(context.filesDir, "$id.mp4").delete()
                preferences.edit().remove(id).commit()
            }
    }

    private fun launchImporter(): ActivityScenario<ImportActivity> =
        ActivityScenario.launch(
            Intent(ApplicationProvider.getApplicationContext(), ImportActivity::class.java)
                .setData(Uri.parse("android.resource://${context.packageName}/raw/lapaflick")))

    private fun rangeLabelText(): String? = try {
        var captured: String? = null
        onView(withId(R.id.importRangeLabel)).check { view, _ ->
            captured = (view as TextView).text.toString()
        }
        captured
    } catch (exception: Throwable) {
        null
    }

    private fun waitForPreparedRange(): String {
        repeat(100) {
            val text = rangeLabelText()
            if (!text.isNullOrBlank() && text.contains("Start")) return text
            Thread.sleep(100)
        }
        throw AssertionError("Importer never prepared the clip")
    }

    @Test
    fun importerShowsTrimRange() {
        launchImporter().use {
            val text = waitForPreparedRange()
            onView(withId(R.id.importRangeLabel)).check(matches(isDisplayed()))
            assertTrue(text, text.contains("End"))
        }
    }

    @Test
    fun previewButtonTogglesPlayAndPause() {
        launchImporter().use {
            waitForPreparedRange()
            onView(withId(R.id.importPreviewButton)).perform(click())
            onView(withText("Pause preview")).check(matches(isDisplayed()))
            onView(withId(R.id.importPreviewButton)).perform(click())
            onView(withText("Preview selection")).check(matches(isDisplayed()))
        }
    }

    @Test
    fun nudgingStartUpdatesTheRangeLabel() {
        launchImporter().use {
            val before = waitForPreparedRange()
            onView(withId(R.id.importNudgeStartPlus)).perform(click())
            val after = rangeLabelText()
            assertTrue("Range label did not change: $before -> $after", before != after)
        }
    }

    @Test
    fun scrubButtonsRespond() {
        launchImporter().use {
            waitForPreparedRange()
            onView(withId(R.id.importRewind)).perform(click())
            onView(withId(R.id.importForward)).perform(click())
            onView(withId(R.id.importRangeLabel)).check(matches(isDisplayed()))
        }
    }

    @Test
    fun savingCreatesAnImportedMove() {
        launchImporter().use {
            waitForPreparedRange()
            onView(withId(R.id.importNameInput)).perform(replaceText(moveName))
            onView(withId(R.id.importSaveButton)).perform(click())
            val preferences = context.getSharedPreferences("imports", Context.MODE_PRIVATE)
            var id: String? = null
            for (attempt in 0 until 150) {
                id = preferences.all.entries.firstOrNull { it.value == moveName }?.key
                if (id != null) break
                Thread.sleep(100)
            }
            assertTrue("Move was not saved", id != null)
            assertTrue("Trimmed file missing", File(context.filesDir, "$id.mp4").exists())
        }
    }
}
