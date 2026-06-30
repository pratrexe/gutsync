package com.example.gutsync

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.example.gutsync.data.NutrientData
import com.google.firebase.Firebase
import com.google.firebase.ai.ai
import com.google.firebase.ai.type.Schema
import com.google.firebase.ai.type.generationConfig
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch
import org.json.JSONObject

import com.example.gutsync.data.ChatMessage
import com.example.gutsync.data.MessageRole

class GutSyncViewModel : ViewModel() {
    private val _uiState: MutableStateFlow<UiState> =
        MutableStateFlow(UiState.Initial)
    val uiState: StateFlow<UiState> =
        _uiState.asStateFlow()

    private val _chatHistory = MutableStateFlow<List<ChatMessage>>(emptyList())
    val chatHistory: StateFlow<List<ChatMessage>> = _chatHistory.asStateFlow()

    private val _analyzedFood: MutableStateFlow<NutrientData?> = MutableStateFlow(null)
    val analyzedFood: StateFlow<NutrientData?> = _analyzedFood.asStateFlow()

    private val generativeModel = Firebase.ai.generativeModel(
        modelName = "gemini-2.5-flash",
        generationConfig = generationConfig {
            responseMimeType = "application/json"
            responseSchema = Schema.obj(
                mapOf(
                    "foodName" to Schema.string("Name of the food"),
                    "calories" to Schema.integer("Total calories"),
                    "fiber" to Schema.double("Fiber in grams"),
                    "saturatedFats" to Schema.double("Saturated fats in grams"),
                    "refinedSugars" to Schema.double("Refined sugars in grams"),
                    "animalProtein" to Schema.double("Animal protein in grams"),
                    "polyphenols" to Schema.double("Polyphenols in mg"),
                    "fermentedCultures" to Schema.integer("Count of active fermented cultures")
                )
            )
        }
    )

    fun analyzeFood(description: String) {
        _uiState.value = UiState.Loading
        viewModelScope.launch(Dispatchers.IO) {
            try {
                val prompt = """
                    Analyze the following food description and provide its nutritional components.
                    If the description is vague, provide estimates for a standard serving.
                    Description: $description
                """.trimIndent()
                
                val response = generativeModel.generateContent(prompt)
                response.text?.let { jsonString ->
                    val json = JSONObject(jsonString)
                    val nutrientData = NutrientData(
                        foodName = json.optString("foodName"),
                        calories = json.optInt("calories"),
                        fiber = json.optDouble("fiber").toFloat(),
                        saturatedFats = json.optDouble("saturatedFats").toFloat(),
                        refinedSugars = json.optDouble("refinedSugars").toFloat(),
                        animalProtein = json.optDouble("animalProtein").toFloat(),
                        polyphenols = json.optDouble("polyphenols").toFloat(),
                        fermentedCultures = json.optInt("fermentedCultures")
                    )
                    _analyzedFood.value = nutrientData
                    _uiState.value = UiState.Success("Analyzed ${nutrientData.foodName}")
                }
            } catch (e: Exception) {
                _uiState.value = UiState.Error(e.localizedMessage ?: "An error occurred during analysis")
            }
        }
    }

    fun askFoodQuestion(question: String) {
        // Add user message to history
        _chatHistory.value = _chatHistory.value + ChatMessage(question, MessageRole.USER)
        
        _uiState.value = UiState.Loading
        val chatModel = Firebase.ai.generativeModel(modelName = "gemini-2.5-flash")
        
        viewModelScope.launch(Dispatchers.IO) {
            try {
                val prompt = "You are a friendly microbiome health expert for the GutSync app named Cooper. " +
                        "If the user says 'hi' or greets you, respond with a natural, friendly greeting. " +
                        "Otherwise, answer this question about food and gut health concisely: $question. " +
                        "Include emojis. " +
                        "DO NOT use markdown symbols like '*' or '#' for formatting."
                val response = chatModel.generateContent(prompt)
                response.text?.let { outputContent ->
                    val cleanContent = outputContent
                        .replace("**", "")
                        .replace("*", "")
                        .replace("#", "")
                    
                    // Add model message to history
                    _chatHistory.value = _chatHistory.value + ChatMessage(cleanContent, MessageRole.MODEL)
                    _uiState.value = UiState.Success(cleanContent)
                }
            } catch (e: Exception) {
                _uiState.value = UiState.Error(e.localizedMessage ?: "An error occurred")
            }
        }
    }

    fun generateMicrobiomeReport(foodItems: List<String>) {
        _uiState.value = UiState.Loading
        val chatModel = Firebase.ai.generativeModel(modelName = "gemini-2.5-flash")

        viewModelScope.launch(Dispatchers.IO) {
            try {
                val prompt = "Based on the following food items logged today: ${foodItems.joinToString()}, " +
                        "provide a concise microbiome health report focusing on shifts in Bifidobacterium, " +
                        "Lactobacillus, Akkermansia, and Bacteroides. Keep it professional and encouraging."
                val response = chatModel.generateContent(prompt)
                response.text?.let { outputContent ->
                    // Clean up markdown symbols like ** and # manually to ensure they don't appear in UI
                    val cleanContent = outputContent
                        .replace("**", "")
                        .replace("*", "")
                        .replace("#", "")
                    _uiState.value = UiState.Success(cleanContent)
                }
            } catch (e: Exception) {
                _uiState.value = UiState.Error(e.localizedMessage ?: "An error occurred")
            }
        }
    }
}
