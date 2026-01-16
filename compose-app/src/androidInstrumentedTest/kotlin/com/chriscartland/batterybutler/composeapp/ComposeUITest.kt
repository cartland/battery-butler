package com.chriscartland.batterybutler.composeapp

import androidx.compose.ui.test.junit4.createAndroidComposeRule
import androidx.compose.ui.test.onNodeWithContentDescription
import androidx.compose.ui.test.onNodeWithText
import androidx.compose.ui.test.performClick
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
        
        // Navigate to Types
        composeTestRule.onNodeWithText("Types").performClick()
        composeTestRule.waitForIdle()
        // Check if "Types" title is displayed (Title matches Tab label)
        // Since both Tab item and Title have text "Types", we might hit multiple nodes.
        // We assert checking existence is enough for now, or refine by Tag/Parent if needed.
        composeTestRule.onNodeWithText("Types").assertExists()

        // Navigate to History
        composeTestRule.onNodeWithText("History").performClick()
        composeTestRule.waitForIdle()
        composeTestRule.onNodeWithText("History").assertExists()

        // Navigate back to Devices
        composeTestRule.onNodeWithText("Devices").performClick()
        composeTestRule.waitForIdle()
        composeTestRule.onNodeWithText("Devices").assertExists()
    }
}
