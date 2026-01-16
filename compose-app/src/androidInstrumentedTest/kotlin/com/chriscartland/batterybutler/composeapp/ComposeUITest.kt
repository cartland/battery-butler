package com.chriscartland.batterybutler.composeapp

import androidx.compose.ui.test.junit4.createAndroidComposeRule
import androidx.compose.ui.test.onNodeWithContentDescription
import androidx.compose.ui.test.onNodeWithText
import androidx.test.ext.junit.runners.AndroidJUnit4
import org.junit.Rule
import org.junit.Test
import org.junit.runner.RunWith

@RunWith(AndroidJUnit4::class)
class ComposeUITest {
    @get:Rule
    val composeTestRule = createAndroidComposeRule<MainActivity>()

    @Test
    fun testAppLaunch() {
        // App launches automatically with createAndroidComposeRule
        composeTestRule.waitForIdle()

        // Check if "Devices" title is displayed (from MainTab.Devices)
        composeTestRule.onNodeWithText("Devices").assertExists()
        
        // Check if Floating Action Button is displayed
        composeTestRule.onNodeWithContentDescription("Add").assertExists()

        // Check for Navigation Bar items
        composeTestRule.onNodeWithText("Types").assertExists()
        composeTestRule.onNodeWithText("History").assertExists()
    }
}
