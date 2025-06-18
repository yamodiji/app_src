package com.appdrawer.fast

import androidx.test.platform.app.InstrumentationRegistry
import androidx.test.ext.junit.runners.AndroidJUnit4
import androidx.test.rule.ActivityTestRule
import androidx.test.espresso.Espresso.onView
import androidx.test.espresso.action.ViewActions.*
import androidx.test.espresso.assertion.ViewAssertions.*
import androidx.test.espresso.matcher.ViewMatchers.*

import org.junit.Test
import org.junit.runner.RunWith
import org.junit.Assert.*
import org.junit.Rule

/**
 * Instrumented test, which will execute on an Android device.
 *
 * See [testing documentation](http://d.android.com/tools/testing).
 */
@RunWith(AndroidJUnit4::class)
class ExampleInstrumentedTest {
    
    @get:Rule
    val activityRule = ActivityTestRule(MainActivity::class.java)
    
    @Test
    fun useAppContext() {
        // Context of the app under test.
        val appContext = InstrumentationRegistry.getInstrumentation().targetContext
        assertEquals("com.appdrawer.fast", appContext.packageName)
    }
    
    @Test
    fun testSearchBoxExists() {
        // Check if search EditText is displayed
        onView(withId(R.id.searchEditText))
            .check(matches(isDisplayed()))
    }
    
    @Test
    fun testRecyclerViewExists() {
        // Check if RecyclerView is displayed
        onView(withId(R.id.recyclerView))
            .check(matches(isDisplayed()))
    }
    
    @Test
    fun testSearchBoxAcceptsText() {
        // Type text in search box
        onView(withId(R.id.searchEditText))
            .perform(typeText("test"))
            .check(matches(withText("test")))
    }
    
    @Test
    fun testSearchBoxClearable() {
        // Type text and then clear it
        onView(withId(R.id.searchEditText))
            .perform(typeText("test"))
            .perform(clearText())
            .check(matches(withText("")))
    }
    
    @Test
    fun testToolbarExists() {
        // Check if toolbar is displayed
        onView(withId(R.id.toolbar))
            .check(matches(isDisplayed()))
    }
} 