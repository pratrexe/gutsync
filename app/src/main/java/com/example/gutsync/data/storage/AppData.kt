package com.example.gutsync.data.storage

import com.example.gutsync.data.ChatSession
import com.example.gutsync.data.NutrientData
import kotlinx.serialization.Serializable

@Serializable
data class AppData(
    val profile: UserProfile = UserProfile(),
    val meals: List<MealLogEntry> = emptyList(),
    val chatSessions: List<ChatSession> = emptyList()
)

@Serializable
data class UserProfile(
    val fiberGoal: Int = 30,
    val polyphenolGoal: Int = 500,
    val resistantStarchGoal: Int = 15
)

@Serializable
data class MealLogEntry(
    val nutrients: NutrientData,
    val timestamp: Long = System.currentTimeMillis(),
    val imageBase64: String? = null,
    val openRouterExplanation: String? = null
)
