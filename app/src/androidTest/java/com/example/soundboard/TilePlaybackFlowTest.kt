package com.example.soundboard

import android.content.Context
import androidx.recyclerview.widget.RecyclerView
import androidx.test.espresso.Espresso.onView
import androidx.test.espresso.Espresso.pressBack
import androidx.test.espresso.action.ViewActions.click
import androidx.test.espresso.assertion.ViewAssertions.matches
import androidx.test.espresso.contrib.RecyclerViewActions
import androidx.test.espresso.matcher.ViewMatchers.hasDescendant
import androidx.test.espresso.matcher.ViewMatchers.isDisplayed
import androidx.test.espresso.matcher.ViewMatchers.withContentDescription
import androidx.test.espresso.matcher.ViewMatchers.withId
import androidx.test.espresso.matcher.ViewMatchers.withText
import androidx.test.ext.junit.rules.ActivityScenarioRule
import androidx.test.ext.junit.runners.AndroidJUnit4
import androidx.test.filters.LargeTest
import androidx.test.platform.app.InstrumentationRegistry
import org.junit.Assert.assertEquals
import org.junit.Assert.assertNotEquals
import org.junit.Rule
import org.junit.Test
import org.junit.runner.RunWith

@RunWith(AndroidJUnit4::class)
@LargeTest
class TilePlaybackFlowTest {

    @get:Rule
    val activityRule = ActivityScenarioRule(MainActivity::class.java)

    private val context: Context
        get() = InstrumentationRegistry.getInstrumentation().targetContext

    private fun openMove(name: String) {
        onView(withId(R.id.moveGrid)).perform(
            RecyclerViewActions.actionOnItem<RecyclerView.ViewHolder>(
                hasDescendant(withText(name)), click()))
    }

    @Test
    fun tappingTileOpensThePlayer() {
        openMove("Carousel")
        onView(withContentDescription("Close video")).check(matches(isDisplayed()))
    }

    @Test
    fun backFromPlayerReturnsToTheGrid() {
        openMove("Carousel")
        onView(withContentDescription("Close video")).check(matches(isDisplayed()))
        pressBack()
        onView(withId(R.id.moveGrid)).check(matches(isDisplayed()))
    }

    @Test
    fun soundToggleFlipsAndPersistsThePreference() {
        val preferences = context.getSharedPreferences(
            MainActivity.SOUND_PREFS, Context.MODE_PRIVATE)
        val before = preferences.getBoolean(MainActivity.KEY_SOUND_ENABLED, true)
        onView(withId(R.id.soundToggle)).perform(click())
        assertNotEquals(before, preferences.getBoolean(MainActivity.KEY_SOUND_ENABLED, true))
        onView(withId(R.id.soundToggle)).perform(click())
        assertEquals(before, preferences.getBoolean(MainActivity.KEY_SOUND_ENABLED, true))
    }
}
