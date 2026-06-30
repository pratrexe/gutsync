package com.example.gutsync.data

enum class MessageRole {
    USER, MODEL
}

data class ChatMessage(
    val text: String,
    val role: MessageRole,
    val timestamp: String = "Just now"
)
