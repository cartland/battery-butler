package com.chriscartland.batterybutler.presentationfeature.aichat

/**
 * UI model for a chat message displayed in [AiChatContent].
 */
data class ChatUiMessage(
    val id: String,
    val text: String,
    val isUser: Boolean,
)
