package com.example.gutsync

import android.app.Application
import android.content.Context
import android.graphics.Bitmap
import android.util.Base64
import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.viewModelScope
import com.example.gutsync.data.ChatMessage
import com.example.gutsync.data.ChatSession
import com.example.gutsync.data.GroqClient
import com.example.gutsync.data.MessageRole
import com.example.gutsync.data.NutrientData
import com.example.gutsync.data.auth.AuthSession
import com.example.gutsync.data.storage.AppData
import com.example.gutsync.data.storage.GutSyncRepository
import com.google.android.gms.auth.api.signin.GoogleSignInAccount
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.*
import kotlinx.coroutines.launch
import org.json.JSONObject
import java.io.ByteArrayOutputStream

class GutSyncViewModel(application: Application) : AndroidViewModel(application) {
    private val repository = GutSyncRepository(application)

    private val _analysisState: MutableStateFlow<UiState> =
        MutableStateFlow(UiState.Initial)
    val analysisState: StateFlow<UiState> =
        _analysisState.asStateFlow()

    private val _chatState: MutableStateFlow<UiState> =
        MutableStateFlow(UiState.Initial)
    val chatState: StateFlow<UiState> =
        _chatState.asStateFlow()

    val appData: StateFlow<AppData> = repository.appData

    private val _analyzedFood: MutableStateFlow<NutrientData?> = MutableStateFlow(null)
    val analyzedFood: StateFlow<NutrientData?> = _analyzedFood.asStateFlow()

    private val _capturedImage = MutableStateFlow<Bitmap?>(null)
    val capturedImage: StateFlow<Bitmap?> = _capturedImage.asStateFlow()

    private val _currentSession = MutableStateFlow(ChatSession())
    val currentSession: StateFlow<ChatSession> = _currentSession.asStateFlow()

    val chatHistory: StateFlow<List<ChatSession>> = repository.appData
        .map { it.chatSessions }
        .stateIn(viewModelScope, SharingStarted.Lazily, emptyList())

    // Pure Groq Models
    private val groqTextModel = "llama-3.3-70b-versatile"
    private val groqVisionModel = "meta-llama/llama-4-scout-17b-16e-instruct"

    private fun Bitmap.toBase64(): String {
        val outputStream = ByteArrayOutputStream()
        this.compress(Bitmap.CompressFormat.JPEG, 80, outputStream)
        return Base64.encodeToString(outputStream.toByteArray(), Base64.NO_WRAP)
    }

    fun setCapturedImage(bitmap: Bitmap?) {
        _capturedImage.value = bitmap
        _analyzedFood.value = null
    }

    fun startNewChat() {
        _currentSession.value = ChatSession()
        _chatState.value = UiState.Initial
    }
    
    fun openSession(session: ChatSession) {
        _currentSession.value = session
        _chatState.value = if (session.messages.isNotEmpty()) UiState.Success("Loaded session") else UiState.Initial
    }

    fun analyzeFood(description: String, bitmap: Bitmap? = null) {
        _analysisState.value = UiState.Loading
        viewModelScope.launch(Dispatchers.IO) {
            try {
                val giePrompt = """
                    Act as the Gut Intelligence Engine (GIE). 
                    Your task is to analyze the food in the provided image or description: "$description".
                    
                    STEP 1: Identify the food items and their exact quantities.
                    STEP 2: Use your internal high-fidelity knowledge base of USDA FoodData Central and Open Food Facts to find matching nutrient profiles.
                    STEP 3: Calculate the impact on the human gut microbiome.
                    
                    Return ONLY a JSON object with this precise schema:
                    {
                      "foodName": "string (The most accurate common name found in USDA/OFF)",
                      "calories": integer,
                      "fiber": float,
                      "resistantStarch": float,
                      "sugar": float,
                      "saturatedFats": float,
                      "animalProtein": float,
                      "polyphenols": float (mg),
                      "fermentedStatus": boolean,
                      "mainPrebioticCompound": "string (e.g., Inulin, Pectin, Beta-Glucan)",
                      "sourceFound": "string (either 'USDA FoodData Central' or 'Open Food Facts')"
                    }
                """.trimIndent()
                
                val base64 = bitmap?.toBase64()
                val resultText = GroqClient.generateContent(
                    prompt = giePrompt, 
                    model = if (base64 != null) groqVisionModel else groqTextModel, 
                    isJson = true,
                    base64Image = base64
                )

                val json = JSONObject(resultText)
                    val nutrientData = NutrientData(
                        foodName = json.optString("foodName"),
                        calories = json.optInt("calories"),
                        fiber = json.optDouble("fiber").toFloat(),
                        resistantStarch = json.optDouble("resistantStarch").toFloat(),
                        sugar = json.optDouble("sugar").toFloat(),
                        saturatedFats = json.optDouble("saturatedFats").toFloat(),
                        animalProtein = json.optDouble("animalProtein").toFloat(),
                        polyphenols = json.optDouble("polyphenols").toFloat(),
                        fermentedStatus = json.optBoolean("fermentedStatus"),
                        mainPrebioticCompound = json.optString("mainPrebioticCompound"),
                        sourceFound = json.optString("sourceFound")
                    )
                    _analyzedFood.value = nutrientData
                    _analysisState.value = UiState.Success("GIE Analysis Complete")
            } catch (e: Exception) {
                _analysisState.value = UiState.Error(e.localizedMessage ?: "GIE Analysis Failed")
            }
        }
    }

    fun addAnalyzedFood() {
        analyzedFood.value?.let { nutrients ->
            val base64 = capturedImage.value?.toBase64()
            viewModelScope.launch {
                repository.addMeal(nutrients, imageBase64 = base64)
                _analyzedFood.value = null
                _capturedImage.value = null
            }
        }
    }

    fun logManualMeal(nutrients: NutrientData, bitmap: Bitmap? = null) {
        val base64 = bitmap?.toBase64()
        viewModelScope.launch {
            repository.addMeal(nutrients, imageBase64 = base64)
        }
    }

    fun askFoodQuestion(question: String) {
        val userMsg = ChatMessage(text = question, role = MessageRole.USER)
        val updatedMsgs = _currentSession.value.messages + userMsg
        _currentSession.value = _currentSession.value.copy(messages = updatedMsgs)
        
        _chatState.value = UiState.Loading
        
        viewModelScope.launch(Dispatchers.IO) {
            try {
                val prompt = "You are a friendly microbiome health expert named Cooper. " +
                        "Answer concisely using microbiome research: $question. " +
                        "Include emojis. No markdown symbols like '*' or '#'."
                
                val resultText = GroqClient.generateContent(prompt, model = groqTextModel)

                val outputContent = resultText
                    val cleanContent = outputContent
                        .replace("**", "")
                        .replace("*", "")
                        .replace("#", "")
                    
                    val modelMsg = ChatMessage(text = cleanContent, role = MessageRole.MODEL)
                    val finalMsgs = _currentSession.value.messages + modelMsg
                    
                    var summary = _currentSession.value.summary
                    if (summary == "New Conversation" && finalMsgs.size >= 2) {
                        val summaryPrompt = "Summarize in 4 words: $question"
                        summary = GroqClient.generateContent(summaryPrompt, model = groqTextModel)
                            ?.replace("\"", "") ?: "Food Chat"
                    }
                    
                    val finalSession = _currentSession.value.copy(
                        messages = finalMsgs,
                        summary = summary,
                        lastUpdated = System.currentTimeMillis()
                    )
                    
                    _currentSession.value = finalSession
                    repository.updateChatSession(finalSession)
                    _chatState.value = UiState.Success(cleanContent)
            } catch (e: Exception) {
                _chatState.value = UiState.Error(e.localizedMessage ?: "Expert unavailable")
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
