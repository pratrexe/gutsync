package com.example.gutsync.data

import kotlinx.serialization.Serializable

@Serializable
enum class MessageRole {
    USER, MODEL
}

@Serializable
data class ChatMessage(
    val text: String,
    val role: MessageRole,
    val timestamp: String = "Just now"
)

