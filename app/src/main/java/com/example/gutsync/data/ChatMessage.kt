package com.example.gutsync.data

import kotlinx.serialization.Serializable
import java.util.UUID

@Serializable
enum class MessageRole {
    USER, MODEL
}

@Serializable
data class ChatMessage(
    val id: String = UUID.randomUUID().toString(),
    val text: String,
    val role: MessageRole,
    val timestamp: Long = System.currentTimeMillis()
)

@Serializable
data class ChatSession(
    val id: String = UUID.randomUUID().toString(),
    val summary: String = "New Conversation",
    val messages: List<ChatMessage> = emptyList(),
    val lastUpdated: Long = System.currentTimeMillis()
)
