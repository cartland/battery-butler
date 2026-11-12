package com.chriscartland.batterybutler.presentationfeature.main

import androidx.compose.foundation.layout.Box
import androidx.compose.runtime.CompositionLocalProvider
import androidx.compose.ui.test.ExperimentalTestApi
import androidx.compose.ui.test.assertCountEquals
import androidx.compose.ui.test.onAllNodesWithTag
import androidx.compose.ui.test.onNodeWithTag
import androidx.compose.ui.test.onNodeWithText
import androidx.compose.ui.test.performClick
import androidx.compose.ui.test.runComposeUiTest
import com.chriscartland.batterybutler.presentationcore.theme.BatteryButlerTheme
import com.chriscartland.batterybutler.presentationcore.theme.LocalAiAvailable
import com.chriscartland.batterybutler.presentationfeature.aichat.ChatCard
import com.chriscartland.batterybutler.presentationfeature.aichat.ChatCardItem
import com.chriscartland.batterybutler.presentationfeature.aichat.ChatCardStatus
import com.chriscartland.batterybutler.presentationfeature.aichat.ChatChangeKind
import com.chriscartland.batterybutler.presentationfeature.aichat.ChatItemOutcome
import com.chriscartland.batterybutler.presentationfeature.aichat.ChatMessage
import kotlin.test.Test
import kotlin.test.assertEquals

/**
 * The card buttons in the split-screen chat panel reach the app. Every layer between the button
 * and the view model once had a do-nothing default, and the panel's call site never passed the
 * real handlers, so Approve, Decline and Undo did nothing there for three releases. The handlers
 * are now required parameters; this pins the last link, in the panel people actually use.
 */
@OptIn(ExperimentalTestApi::class)
class MainScreenAiCardUiTest {
    private fun message(card: ChatCard) = ChatMessage(id = "m1", text = "", isUser = false, card = card)

    private val pending = ChatCard(
        id = "card-1",
        items = listOf(ChatCardItem("i1", ChatChangeKind.RECORD, "Record a battery change for Hall Alarm on 2026-11-11")),
        status = ChatCardStatus.PENDING,
    )

    private val applied = ChatCard(
        id = "card-2",
        items = listOf(ChatCardItem("i2", ChatChangeKind.ADD, "Add Porch Light", outcome = ChatItemOutcome.APPLIED)),
        status = ChatCardStatus.APPLIED,
        canUndo = true,
    )

    private fun panel(
        card: ChatCard,
        calls: MutableList<String>,
        processing: Boolean = false,
        steps: List<String> = emptyList(),
    ): @androidx.compose.runtime.Composable () -> Unit =
        {
            BatteryButlerTheme {
                CompositionLocalProvider(LocalAiAvailable provides true) {
                    MainScreenShell(
                        currentTab = MainTab.Devices,
                        onTabSelected = {},
                        onSettingsClick = {},
                        aiMessages = listOf(message(card)),
                        isAiProcessing = processing,
                        aiSteps = steps,
                        isAiExpanded = true,
                        onAiExpandedChange = {},
                        onSendAiMessage = {},
                        onClearAiChat = {},
                        onApproveAiCard = { id, leftOut -> calls += "approve:$id:${leftOut.size}" },
                        onDeclineAiCard = { calls += "decline:$it" },
                        onUndoAiCard = { calls += "undo:$it" },
                        onRetryAi = {},
                        imeVisible = false,
                    ) { modifier, _ -> Box(modifier) }
                }
            }
        }

    @Test
    fun `approve in the chat panel reaches the app`() =
        runComposeUiTest {
            val calls = mutableListOf<String>()
            setContent(panel(pending, calls))
            onNodeWithTag("ai_card_approve").performClick()
            assertEquals(listOf("approve:card-1:0"), calls)
        }

    @Test
    fun `decline in the chat panel reaches the app`() =
        runComposeUiTest {
            val calls = mutableListOf<String>()
            setContent(panel(pending, calls))
            onNodeWithTag("ai_card_decline").performClick()
            assertEquals(listOf("decline:card-1"), calls)
        }

    @Test
    fun `undo in the chat panel reaches the app`() =
        runComposeUiTest {
            val calls = mutableListOf<String>()
            setContent(panel(applied, calls))
            onNodeWithTag("ai_card_undo").performClick()
            assertEquals(listOf("undo:card-2"), calls)
        }

    @Test
    fun `a card being applied shows progress and no buttons to tap again`() =
        runComposeUiTest {
            val calls = mutableListOf<String>()
            setContent(panel(pending.copy(status = ChatCardStatus.APPLYING), calls))
            onNodeWithTag("ai_card_busy").assertExists()
            onAllNodesWithTag("ai_card_approve").assertCountEquals(0)
            onAllNodesWithTag("ai_card_decline").assertCountEquals(0)
        }

    @Test
    fun `the thinking row lists what the assistant has done so far`() =
        runComposeUiTest {
            val calls = mutableListOf<String>()
            setContent(panel(applied, calls, processing = true, steps = listOf("Looked up Hall Alarm", "Drafted: Add Porch Light")))
            onAllNodesWithTag("ai_step").assertCountEquals(2)
            onNodeWithText("Drafted: Add Porch Light").assertExists()
        }
}
