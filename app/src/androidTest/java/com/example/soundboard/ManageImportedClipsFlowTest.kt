package com.example.soundboard

import android.content.Context
import androidx.recyclerview.widget.RecyclerView
import androidx.test.core.app.ActivityScenario
import androidx.test.espresso.Espresso.onView
import androidx.test.espresso.action.ViewActions.click
import androidx.test.espresso.action.ViewActions.longClick
import androidx.test.espresso.assertion.ViewAssertions.doesNotExist
import androidx.test.espresso.assertion.ViewAssertions.matches
import androidx.test.espresso.contrib.RecyclerViewActions
import androidx.test.espresso.matcher.ViewMatchers.hasDescendant
import androidx.test.espresso.matcher.ViewMatchers.isDisplayed
import androidx.test.espresso.matcher.ViewMatchers.withId
import androidx.test.espresso.matcher.ViewMatchers.withText
import androidx.test.ext.junit.runners.AndroidJUnit4
import androidx.test.filters.LargeTest
import androidx.test.platform.app.InstrumentationRegistry
import org.junit.After
import org.junit.Assert.assertFalse
import org.junit.Before
import org.junit.Test
import org.junit.runner.RunWith
import java.io.File

@RunWith(AndroidJUnit4::class)
@LargeTest
class ManageImportedClipsFlowTest {

    private val context: Context
        get() = InstrumentationRegistry.getInstrumentation().targetContext
    private val importedId = "test-imported-move"

    @Before
    fun seedImportedMove() {
        File(context.filesDir, "$importedId.mp4").writeBytes(ByteArray(64))
        context.getSharedPreferences("imports", Context.MODE_PRIVATE).edit()
            .putString(importedId, "Test import")
            .commit()
    }

    @After
    fun clearImportedMove() {
        File(context.filesDir, "$importedId.mp4").delete()
        context.getSharedPreferences("imports", Context.MODE_PRIVATE).edit()
            .remove(importedId)
            .commit()
    }

    private fun longPressTile(name: String) {
        onView(withId(R.id.moveGrid)).perform(
            RecyclerViewActions.scrollTo<RecyclerView.ViewHolder>(hasDescendant(withText(name))))
        onView(withId(R.id.moveGrid)).perform(
            RecyclerViewActions.actionOnItem<RecyclerView.ViewHolder>(
                hasDescendant(withText(name)), longClick()))
    }

    @Test
    fun longPressImportedTileOffersEditAndDelete() {
        ActivityScenario.launch(MainActivity::class.java).use {
            longPressTile("Test import")
            onView(withText("Edit clip")).check(matches(isDisplayed()))
            onView(withText("Delete from Damoves")).check(matches(isDisplayed()))
        }
    }

    @Test
    fun deletingImportedTileRemovesTileAndFile() {
        ActivityScenario.launch(MainActivity::class.java).use {
            longPressTile("Test import")
            onView(withText("Delete from Damoves")).perform(click())
            onView(withText("Delete")).perform(click())
            onView(withText("Test import")).check(doesNotExist())
            assertFalse(File(context.filesDir, "$importedId.mp4").exists())
            assertFalse(context.getSharedPreferences("imports", Context.MODE_PRIVATE)
                .contains(importedId))
        }
    }

    @Test
    fun longPressBundledTileIsInert() {
        ActivityScenario.launch(MainActivity::class.java).use {
            longPressTile("Carousel")
            onView(withText("Edit clip")).check(doesNotExist())
            onView(withId(R.id.moveGrid)).check(matches(isDisplayed()))
        }
    }
}
