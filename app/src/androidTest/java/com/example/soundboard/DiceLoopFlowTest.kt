package com.example.soundboard

import androidx.test.espresso.Espresso.onView
import androidx.test.espresso.action.ViewActions.click
import androidx.test.espresso.action.ViewActions.closeSoftKeyboard
import androidx.test.espresso.action.ViewActions.typeText
import androidx.test.espresso.assertion.ViewAssertions.matches
import androidx.test.espresso.matcher.ViewMatchers.isDisplayed
import androidx.test.espresso.matcher.ViewMatchers.withContentDescription
import androidx.test.espresso.matcher.ViewMatchers.withId
import androidx.test.ext.junit.rules.ActivityScenarioRule
import androidx.test.ext.junit.runners.AndroidJUnit4
import androidx.test.filters.LargeTest
import org.junit.Rule
import org.junit.Test
import org.junit.runner.RunWith

@RunWith(AndroidJUnit4::class)
@LargeTest
class DiceLoopFlowTest {

    @get:Rule
    val activityRule = ActivityScenarioRule(MainActivity::class.java)

    @Test
    fun diceOpensAPlayer() {
        onView(withId(R.id.dice)).perform(click())
        onView(withContentDescription("Close video")).check(matches(isDisplayed()))
    }

    @Test
    fun diceIgnoresTheSearchFilter() {
        onView(withId(R.id.searchInput)).perform(typeText("carousel"), closeSoftKeyboard())
        onView(withId(R.id.dice)).perform(click())
        onView(withContentDescription("Close video")).check(matches(isDisplayed()))
    }
}
