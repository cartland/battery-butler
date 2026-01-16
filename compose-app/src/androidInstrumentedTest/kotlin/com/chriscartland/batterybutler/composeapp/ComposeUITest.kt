package com.chriscartland.batterybutler.composeapp

import androidx.compose.ui.test.assertTextEquals
import androidx.compose.ui.test.junit4.createAndroidComposeRule
import androidx.compose.ui.test.onNodeWithTag
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

        // Check if "Devices" title is displayed (Tag ensures we check TopAppBar)
        composeTestRule.onNodeWithTag("TopBarTitle").assertTextEquals("Devices")

        // Navigate to Types (Click the Bottom Navigation Item specifically)
        composeTestRule.onNodeWithTag("BottomNav_Types").performClick()
        composeTestRule.waitForIdle()
        // Check if "Types" title is displayed in TopAppBar
        composeTestRule.onNodeWithTag("TopBarTitle").assertTextEquals("Types")

        // Navigate to History
        composeTestRule.onNodeWithTag("BottomNav_History").performClick()
        composeTestRule.waitForIdle()
        composeTestRule.onNodeWithTag("TopBarTitle").assertTextEquals("History")

        // Navigate back to Devices
        composeTestRule.onNodeWithTag("BottomNav_Devices").performClick()
        composeTestRule.waitForIdle()
        composeTestRule.onNodeWithTag("TopBarTitle").assertTextEquals("Devices")
    }
}
