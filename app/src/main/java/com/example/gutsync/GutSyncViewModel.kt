package com.example.gutsync

import android.app.Application
import android.content.Context
import android.graphics.Bitmap
import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.viewModelScope
import com.example.gutsync.data.ChatMessage
import com.example.gutsync.data.MessageRole
import com.example.gutsync.data.NutrientData
import com.example.gutsync.data.auth.AuthSession
import com.example.gutsync.data.storage.AppData
import com.example.gutsync.data.storage.GutSyncRepository
import com.google.android.gms.auth.api.signin.GoogleSignInAccount
import com.google.firebase.Firebase
import com.google.firebase.ai.ai
import com.google.firebase.ai.type.Schema
import com.google.firebase.ai.type.Tool
import com.google.firebase.ai.type.content
import com.google.firebase.ai.type.generationConfig
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.*
import kotlinx.coroutines.launch
import org.json.JSONObject

class GutSyncViewModel(application: Application) : AndroidViewModel(application) {
    private val repository = GutSyncRepository(application)

    private val _uiState: MutableStateFlow<UiState> =
        MutableStateFlow(UiState.Initial)
    val uiState: StateFlow<UiState> =
        _uiState.asStateFlow()

    val appData: StateFlow<AppData> = repository.appData

    private val _analyzedFood: MutableStateFlow<NutrientData?> = MutableStateFlow(null)
    val analyzedFood: StateFlow<NutrientData?> = _analyzedFood.asStateFlow()

    private val _capturedImage = MutableStateFlow<Bitmap?>(null)
    val capturedImage: StateFlow<Bitmap?> = _capturedImage.asStateFlow()

    private val _currentChat = MutableStateFlow<List<ChatMessage>>(emptyList())
    val currentChat: StateFlow<List<ChatMessage>> = _currentChat.asStateFlow()

    val chatHistory: StateFlow<List<ChatMessage>> = repository.appData
        .map { it.chats }
        .stateIn(viewModelScope, SharingStarted.Lazily, emptyList())

    private val generativeModel = Firebase.ai.generativeModel(
        modelName = "gemini-2.0-flash",
        generationConfig = generationConfig {
            responseMimeType = "application/json"
            responseSchema = Schema.obj(
                mapOf(
                    "foodName" to Schema.string("Name of the food identified"),
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

    fun setCapturedImage(bitmap: Bitmap?) {
        _capturedImage.value = bitmap
        _analyzedFood.value = null
    }

    fun startNewChat() {
        _currentChat.value = emptyList()
        _uiState.value = UiState.Initial
    }

    fun analyzeFood(description: String, bitmap: Bitmap? = null) {
        _uiState.value = UiState.Loading
        viewModelScope.launch(Dispatchers.IO) {
            try {
                val prompt = """
                    Analyze this food and provide its nutritional components. 
                    Identify exactly what is in the food. 
                    Check scientific data to provide accurate microbiome-active components.
                    If the description is vague, use the image to identify ingredients.
                    Description/Context: $description
                """.trimIndent()
                
                val response = if (bitmap != null) {
                    generativeModel.generateContent(
                        content {
                            image(bitmap)
                            text(prompt)
                        }
                    )
                } else {
                    generativeModel.generateContent(prompt)
                }

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

    fun addAnalyzedFood() {
        analyzedFood.value?.let { nutrients ->
            viewModelScope.launch {
                repository.addMeal(nutrients)
                _analyzedFood.value = null
                _capturedImage.value = null
            }
        }
    }

    fun askFoodQuestion(question: String) {
        val userMsg = ChatMessage(question, MessageRole.USER)
        _currentChat.value = _currentChat.value + userMsg
        
        viewModelScope.launch {
            repository.addChatMessage(userMsg)
        }
        
        _uiState.value = UiState.Loading
        val chatModel = Firebase.ai.generativeModel(modelName = "gemini-2.0-flash")
        
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
                    
                    val modelMsg = ChatMessage(cleanContent, MessageRole.MODEL)
                    _currentChat.value = _currentChat.value + modelMsg
                    
                    viewModelScope.launch {
                        repository.addChatMessage(modelMsg)
                    }
                    _uiState.value = UiState.Success(cleanContent)
                }
            } catch (e: Exception) {
                _uiState.value = UiState.Error(e.localizedMessage ?: "An error occurred")
            }
        }
    }

    fun syncWithDrive(context: Context, account: GoogleSignInAccount, onComplete: (AuthSession?, String?) -> Unit) {
        viewModelScope.launch {
            try {
                val drive = com.example.gutsync.data.auth.GoogleAuthHelper.getDriveService(context, account)
                val newSession = repository.syncWithDrive(drive)
                val finalSession = newSession.copy(
                    isLoggedIn = true,
                    displayName = account.displayName ?: "User",
                    email = account.email,
                    photoUrl = account.photoUrl?.toString(),
                    accountType = com.example.gutsync.data.auth.AccountType.GOOGLE
                )
                com.example.gutsync.data.auth.SessionManager(context).saveSession(finalSession)
                onComplete(finalSession, null)
            } catch (e: Exception) {
                e.printStackTrace()
                onComplete(null, e.localizedMessage ?: "Drive synchronization failed")
            }
        }
    }
}
