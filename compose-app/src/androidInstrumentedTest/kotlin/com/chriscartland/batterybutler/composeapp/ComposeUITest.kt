package com.chriscartland.batterybutler.composeapp

import androidx.compose.ui.test.assertTextEquals
import androidx.compose.ui.test.hasTestTag
import androidx.compose.ui.test.hasText
import androidx.compose.ui.test.junit4.createAndroidComposeRule
import androidx.compose.ui.test.onNodeWithContentDescription
import androidx.compose.ui.test.onNodeWithTag
import androidx.compose.ui.test.onNodeWithText
import androidx.compose.ui.test.performClick
import androidx.compose.ui.test.performTextInput
import androidx.test.ext.junit.runners.AndroidJUnit4
import org.junit.Rule
import org.junit.Test
import org.junit.runner.RunWith

@RunWith(AndroidJUnit4::class)
class ComposeUITest {
    @get:Rule
    val composeTestRule = createAndroidComposeRule<MainActivity>()

    /** Wait for a TopBarTitle with the expected text to appear. */
    private fun assertScreen(expectedTitle: String) {
        composeTestRule.waitUntil(timeoutMillis = 5_000) {
            try {
                composeTestRule
                    .onNode(hasTestTag("TopBarTitle") and hasText(expectedTitle))
                    .assertExists()
                true
            } catch (_: AssertionError) {
                false
            }
        }
    }

    /** Wait for a text node to appear, then click it. */
    private fun waitAndClick(text: String) {
        composeTestRule.waitUntil(timeoutMillis = 5_000) {
            try {
                composeTestRule.onNodeWithText(text).assertExists()
                true
            } catch (_: AssertionError) {
                false
            }
        }
        composeTestRule.onNodeWithText(text).performClick()
        composeTestRule.waitForIdle()
    }

    /** Click the top-bar back arrow (contentDescription "Back"). */
    private fun clickBackArrow() {
        composeTestRule.onNodeWithContentDescription("Back").performClick()
        composeTestRule.waitForIdle()
    }

    /** Skip the login screen. */
    private fun skipLogin() {
        composeTestRule.waitForIdle()
        waitAndClick("Continue without signing in")
        assertScreen("Devices")
    }

    @Test
    fun testAppLaunch() {
        // App launches at Login screen — skip to Devices
        composeTestRule.waitForIdle()
        composeTestRule.onNodeWithText("Continue without signing in").performClick()
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

    /**
     * Smoke test that navigates to every reachable screen in the app.
     * Verifies screens render without crashing, not functional correctness.
     * Sequential because later steps depend on data created in earlier steps.
     */
    @Test
    fun testNavigateAllScreens() {
        // ── Phase 1: Login → Devices ──
        skipLogin()

        // ── Phase 2: Settings ──
        composeTestRule.onNodeWithContentDescription("Settings").performClick()
        composeTestRule.waitForIdle()
        assertScreen("Settings")
        clickBackArrow()

        // ── Phase 3: Types tab → AddDeviceType → create data ──
        composeTestRule.onNodeWithTag("BottomNav_Types").performClick()
        composeTestRule.waitForIdle()
        assertScreen("Types")

        waitAndClick("Add Type")
        assertScreen("New Device Type")

        // Name field uses placeholder (not label) — find by placeholder text
        composeTestRule.onNodeWithText("e.g. Xbox Controller").performTextInput("Test Type")
        composeTestRule.waitForIdle()
        waitAndClick("Save")
        assertScreen("Types")

        // ── Phase 4: DeviceTypeDetail → EditDeviceType ──
        waitAndClick("Test Type")
        assertScreen("Device Type")
        waitAndClick("Edit")
        assertScreen("Edit Device Type")
        waitAndClick("Cancel")
        assertScreen("Device Type")
        clickBackArrow()
        assertScreen("Types")

        // ── Phase 5: Devices tab → AddDevice → create a device ──
        composeTestRule.onNodeWithTag("BottomNav_Devices").performClick()
        composeTestRule.waitForIdle()
        assertScreen("Devices")

        waitAndClick("Add Device")
        assertScreen("Add Device")
        composeTestRule.onNodeWithText("Device Name").performTextInput("Test Device")
        composeTestRule.waitForIdle()
        // Select device type from dropdown
        composeTestRule.onNodeWithText("Device Type").performClick()
        composeTestRule.waitForIdle()
        waitAndClick("Test Type")
        waitAndClick("Save")
        assertScreen("Devices")

        // ── Phase 6: DeviceDetail ──
        waitAndClick("Test Device")
        assertScreen("Device Details")

        // ── Phase 7: EditDevice ──
        waitAndClick("Edit")
        assertScreen("Edit Device")
        waitAndClick("Cancel")
        assertScreen("Device Details")

        // ── Phase 8: Record Replacement to create a battery event ──
        waitAndClick("Record Replacement")
        clickBackArrow()

        // ── Phase 9: History tab → AddBatteryEvent ──
        composeTestRule.onNodeWithTag("BottomNav_History").performClick()
        composeTestRule.waitForIdle()
        assertScreen("History")

        waitAndClick("Add a battery event")
        assertScreen("Add Battery Event")
        clickBackArrow()

        // ── Phase 10: EventDetail (read-only) → EditBatteryEvent ──
        waitAndClick("Test Device")
        assertScreen("Event Detail")
        waitAndClick("Edit")
        assertScreen("Edit Event")
        waitAndClick("Cancel")
        assertScreen("Event Detail")
        clickBackArrow()
        assertScreen("History")
    }
}
