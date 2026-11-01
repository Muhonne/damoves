package com.example.soundboard

import androidx.recyclerview.widget.RecyclerView
import androidx.test.espresso.Espresso.onView
import androidx.test.espresso.action.ViewActions.click
import androidx.test.espresso.action.ViewActions.closeSoftKeyboard
import androidx.test.espresso.action.ViewActions.typeText
import androidx.test.espresso.assertion.ViewAssertions.matches
import androidx.test.espresso.matcher.ViewMatchers.isDisplayed
import androidx.test.espresso.matcher.ViewMatchers.withId
import androidx.test.espresso.matcher.ViewMatchers.withText
import androidx.test.ext.junit.rules.ActivityScenarioRule
import androidx.test.ext.junit.runners.AndroidJUnit4
import androidx.test.filters.LargeTest
import org.hamcrest.CoreMatchers.not
import org.junit.Rule
import org.junit.Test
import org.junit.runner.RunWith

@RunWith(AndroidJUnit4::class)
@LargeTest
class BrowseAndSearchFlowTest {

    @get:Rule
    val activityRule = ActivityScenarioRule(MainActivity::class.java)

    @Test
    fun gridShowsBundledMoves() {
        onView(withId(R.id.moveGrid)).check(matches(isDisplayed()))
        onView(withText("Carousel")).check(matches(isDisplayed()))
        onView(withId(R.id.moveGrid)).check { view, noViewFoundException ->
            noViewFoundException?.let { throw it }
            val tile = (view as RecyclerView).getChildAt(0) as MoveTileView
            org.junit.Assert.assertEquals(0.75, tile.width.toDouble() / tile.height.toDouble(), 0.01)
        }
    }

    @Test
    fun searchFiltersTheGrid() {
        onView(withId(R.id.searchInput)).perform(typeText("carousel"), closeSoftKeyboard())
        onView(withText("Carousel")).check(matches(isDisplayed()))
    }

    @Test
    fun noMatchShowsEmptyStateWithClearAction() {
        onView(withId(R.id.searchInput)).perform(typeText("nomatchqqq"), closeSoftKeyboard())
        onView(withId(R.id.emptyState)).check(matches(isDisplayed()))
        onView(withText(R.string.no_moves_found)).check(matches(isDisplayed()))
        onView(withId(R.id.clearSearchButton)).perform(click())
        onView(withId(R.id.emptyState)).check(matches(not(isDisplayed())))
        onView(withText("Carousel")).check(matches(isDisplayed()))
    }

    @Test
    fun querySurvivesRecreation() {
        onView(withId(R.id.searchInput)).perform(typeText("carousel"), closeSoftKeyboard())
        activityRule.scenario.recreate()
        onView(withText("Carousel")).check(matches(isDisplayed()))
    }

    @Test
    fun clearingFromEmptyStateRestoresTheGrid() {
        onView(withId(R.id.searchInput)).perform(typeText("nomatchqqq"))
        onView(withId(R.id.emptyState)).check(matches(isDisplayed()))
        onView(withId(R.id.clearSearchButton)).perform(click())
        onView(withId(R.id.emptyState)).check(matches(not(isDisplayed())))
        onView(withText("Carousel")).check(matches(isDisplayed()))
        onView(withText("Inside turn leader turn")).check(matches(isDisplayed()))
    }
}
