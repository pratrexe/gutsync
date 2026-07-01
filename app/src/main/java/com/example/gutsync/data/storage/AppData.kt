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
    val healthScore: Int = 84,
    val fiberGoal: Int = 35,
    val currentFiber: Int = 24,
    val growthPercentage: Int = 12
)

@Serializable
data class MealLogEntry(
    val nutrients: NutrientData,
    val timestamp: Long = System.currentTimeMillis(),
    val imageBase64: String? = null
)
