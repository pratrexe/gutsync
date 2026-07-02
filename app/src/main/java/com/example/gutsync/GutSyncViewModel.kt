package com.example.gutsync

import android.app.Application
import android.content.Context
import android.graphics.Bitmap
import android.util.Log
import android.util.Base64
import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.viewModelScope
import com.example.gutsync.data.ChatMessage
import com.example.gutsync.data.ChatSession
import com.example.gutsync.data.GroqClient
import com.example.gutsync.data.OFFClient
import com.example.gutsync.data.MessageRole
import com.example.gutsync.data.NutrientData
import com.example.gutsync.data.MicrobeImpactCalculator
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

    private val _openRouterExplanation = MutableStateFlow<String?>(null)
    val openRouterExplanation: StateFlow<String?> = _openRouterExplanation.asStateFlow()

    private val _capturedImage = MutableStateFlow<Bitmap?>(null)
    val capturedImage: StateFlow<Bitmap?> = _capturedImage.asStateFlow()

    private val _chatImage = MutableStateFlow<Bitmap?>(null)
    val chatImage: StateFlow<Bitmap?> = _chatImage.asStateFlow()

    private val _currentSession = MutableStateFlow(ChatSession())
    val currentSession: StateFlow<ChatSession> = _currentSession.asStateFlow()

    val chatHistory: StateFlow<List<ChatSession>> = repository.appData
        .map { it.chatSessions }
        .stateIn(viewModelScope, SharingStarted.Lazily, emptyList())

    // AI Models
    private val gemmaModel = "google/gemma-4-31b-it:free"
    private val groqTextModel = "llama-3.3-70b-versatile"

    private fun Bitmap.toBase64(): String {
        val scaled = Bitmap.createScaledBitmap(this, 1024, (height * 1024f / width).toInt(), true)
        val outputStream = ByteArrayOutputStream()
        scaled.compress(Bitmap.CompressFormat.JPEG, 85, outputStream)
        return Base64.encodeToString(outputStream.toByteArray(), Base64.NO_WRAP)
    }

    fun setCapturedImage(bitmap: Bitmap?) {
        _capturedImage.value = bitmap
        _analyzedFood.value = null
        _identifiedFoodName.value = null
    }

    fun setChatImage(bitmap: Bitmap?) {
        _chatImage.value = bitmap
    }

    fun startNewChat() {
        _currentSession.value = ChatSession()
        _chatState.value = UiState.Initial
    }
    
    fun openSession(session: ChatSession) {
        _currentSession.value = session
        _chatState.value = if (session.messages.isNotEmpty()) UiState.Success("Loaded session") else UiState.Initial
    }

    fun toggleSessionModel() {
        val current = _currentSession.value
        val newModel = if (current.preferredModel == "Groq") "OpenRouter" else "Groq"
        val updated = current.copy(preferredModel = newModel)
        _currentSession.value = updated
        viewModelScope.launch {
            repository.updateChatSession(updated)
        }
    }

    fun analyzeBarcode(barcode: String) {
        _analysisState.value = UiState.Loading
        viewModelScope.launch(Dispatchers.IO) {
            try {
                val nutrientData = OFFClient.getProductByBarcode(barcode)
                if (nutrientData != null) {
                    val scorecard = MicrobeImpactCalculator.calculateGIE(nutrientData)
                    val explanationPrompt = """
                        Explain this Gut Health Score for ${nutrientData.foodName}: ${scorecard.gutHealthScore}/100.
                        Nutrients (from Open Food Facts): Fiber: ${nutrientData.fiber}g, Sugar: ${nutrientData.sugar}g.
                        Mention impact on Bifidobacterium, Lactobacillus, Akkermansia, and Bacteroides.
                        Keep it concise and scientific. No markdown.
                    """.trimIndent()
                    
                    val explanation = GroqClient.generateContent(
                        prompt = explanationPrompt,
                        model = gemmaModel,
                        isJson = false
                    ).replace("*", "").replace("#", "")

                    _openRouterExplanation.value = explanation
                    _analyzedFood.value = nutrientData
                    _analysisState.value = UiState.Success("Barcode Analysis Complete")
                } else {
                    _analysisState.value = UiState.Error("Product not found in Open Food Facts database.")
                }
            } catch (e: Exception) {
                _analysisState.value = UiState.Error("Barcode search failed: ${e.localizedMessage}")
            }
        }
    }

    private val _identifiedFoodName = MutableStateFlow<String?>(null)
    val identifiedFoodName: StateFlow<String?> = _identifiedFoodName.asStateFlow()

    fun identifyFoodFromPhoto(bitmap: Bitmap) {
        _analysisState.value = UiState.Loading
        viewModelScope.launch(Dispatchers.IO) {
            try {
                val visionPrompt = "Identify the primary food item in this image. Return ONLY the name of the food, nothing else."
                val base64 = bitmap.toBase64()
                val result = GroqClient.generateContent(
                    prompt = visionPrompt,
                    model = gemmaModel,
                    isJson = false,
                    base64Image = base64
                ).trim().replace(".", "").replace("*", "")
                
                _identifiedFoodName.value = result
                _analysisState.value = UiState.Initial
            } catch (e: Exception) {
                _analysisState.value = UiState.Error("Identification failed: ${e.localizedMessage}")
            }
        }
    }

    fun analyzeFood(description: String, bitmap: Bitmap? = null, quantityGrams: Float = 100f) {
        _analysisState.value = UiState.Loading
        viewModelScope.launch(Dispatchers.IO) {
            try {
                // STEP 1: Use Groq (Llama 70B) for high-speed nutritional lookup based on the name/description
                val searchPrompt = """
                    Act as the Gut Intelligence Engine. Analyze the food: $description for EXACTLY $quantityGrams grams.
                    Return ONLY a JSON object with this schema:
                    {
                      "foodName": "string",
                      "calories": integer,
                      "protein": float,
                      "carbs": float,
                      "totalFat": float,
                      "fiber": float,
                      "resistantStarch": float,
                      "sugar": float,
                      "saturatedFats": float,
                      "animalProtein": float,
                      "polyphenols": float,
                      "fermentedStatus": boolean,
                      "additives": ["string"],
                      "mainPrebioticCompound": "string",
                      "sourceFound": "Groq Llama 3.3 70B"
                    }
                """.trimIndent()
                
                val resultText = GroqClient.generateContent(
                    prompt = searchPrompt, 
                    model = groqTextModel, 
                    isJson = true
                )

                // Robust JSON extraction
                val jsonStart = resultText.indexOf("{")
                val jsonEnd = resultText.lastIndexOf("}")
                val cleanJsonStr = if (jsonStart != -1 && jsonEnd != -1) {
                    resultText.substring(jsonStart, jsonEnd + 1)
                } else resultText

                val json = JSONObject(cleanJsonStr)
                val nutrientData = NutrientData(
                    foodName = json.optString("foodName"),
                    calories = json.optInt("calories"),
                    protein = json.optDouble("protein").toFloat(),
                    carbs = json.optDouble("carbs").toFloat(),
                    totalFat = json.optDouble("totalFat").toFloat(),
                    fiber = json.optDouble("fiber").toFloat(),
                    resistantStarch = json.optDouble("resistantStarch").toFloat(),
                    sugar = json.optDouble("sugar").toFloat(),
                    saturatedFats = json.optDouble("saturatedFats").toFloat(),
                    animalProtein = json.optDouble("animalProtein").toFloat(),
                    polyphenols = json.optDouble("polyphenols").toFloat(),
                    fermentedStatus = json.optBoolean("fermentedStatus"),
                    additives = mutableListOf<String>().apply {
                        val array = json.optJSONArray("additives")
                        if (array != null) {
                            for (i in 0 until array.length()) {
                                add(array.getString(i))
                            }
                        }
                    },
                    mainPrebioticCompound = json.optString("mainPrebioticCompound"),
                    sourceFound = json.optString("sourceFound")
                )
                
                val scorecard = MicrobeImpactCalculator.calculateGIE(nutrientData)
                
                // STEP 2: Use Gemma (OpenRouter) for the scientific explanation (high reasoning)
                val explanationPrompt = """
                    Explain this Gut Health Score: ${scorecard.gutHealthScore}/100 for $quantityGrams grams of ${nutrientData.foodName}.
                    Microbe Shifts: ${scorecard.predictedShifts.joinToString { shift -> "${shift.microbeType.displayName}: ${shift.shiftPercentage}%" }}
                    Focus on Bifidobacterium, Lactobacillus, Akkermansia, and Bacteroides impact.
                    Keep it concise and scientific. No markdown symbols.
                """.trimIndent()
                
                val explanation = GroqClient.generateContent(
                    prompt = explanationPrompt,
                    model = gemmaModel,
                    isJson = false
                ).replace("*", "").replace("#", "")

                _openRouterExplanation.value = explanation
                _analyzedFood.value = nutrientData
                _identifiedFoodName.value = null
                _analysisState.value = UiState.Success("Analysis Complete")
            } catch (e: Exception) {
                Log.e("GutSyncViewModel", "Analysis Error", e)
                _analysisState.value = UiState.Error(e.localizedMessage ?: "Analysis Failed")
            }
        }
    }

    fun addAnalyzedFood() {
        analyzedFood.value?.let { nutrients ->
            val base64 = capturedImage.value?.toBase64()
            val explanation = openRouterExplanation.value
            viewModelScope.launch {
                repository.addMeal(nutrients, imageBase64 = base64, openRouterExplanation = explanation)
                _analyzedFood.value = null
                _capturedImage.value = null
                _openRouterExplanation.value = null
                _identifiedFoodName.value = null
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
        val bitmap = _chatImage.value
        val userMsg = ChatMessage(text = question, role = MessageRole.USER)
        val updatedMsgs = _currentSession.value.messages + userMsg
        _currentSession.value = _currentSession.value.copy(messages = updatedMsgs)
        _chatImage.value = null
        _chatState.value = UiState.Loading
        
        viewModelScope.launch(Dispatchers.IO) {
            try {
                val modelToUse = if (_currentSession.value.preferredModel == "OpenRouter") gemmaModel else groqTextModel
                val prompt = "You are a friendly microbiome health expert named Cooper. Answer concisely: $question. No markdown."
                val base64 = bitmap?.toBase64()
                
                val modelMsg = ChatMessage(text = "", role = MessageRole.MODEL)
                val msgsWithPlaceholder = _currentSession.value.messages + modelMsg
                _currentSession.value = _currentSession.value.copy(messages = msgsWithPlaceholder)
                
                var fullContent = ""
                GroqClient.generateContentStream(
                    prompt = prompt, 
                    model = modelToUse,
                    base64Image = base64
                ).collect { chunk ->
                    fullContent += chunk
                    val cleanContent = fullContent.replace("*", "").replace("#", "")
                    val currentMsgs = _currentSession.value.messages.toMutableList()
                    if (currentMsgs.isNotEmpty()) {
                        currentMsgs[currentMsgs.size - 1] = ChatMessage(text = cleanContent, role = MessageRole.MODEL)
                        _currentSession.value = _currentSession.value.copy(messages = currentMsgs)
                    }
                    _chatState.value = UiState.Success(cleanContent)
                }

                val finalSession = _currentSession.value
                var summary = finalSession.summary
                if (summary == "New Conversation" && finalSession.messages.size >= 2) {
                    val summaryPrompt = "Summarize in 4 words: $question"
                    summary = GroqClient.generateContent(summaryPrompt, model = groqTextModel).replace("\"", "")
                }
                    
                val finalUpdatedSession = finalSession.copy(summary = summary, lastUpdated = System.currentTimeMillis())
                _currentSession.value = finalUpdatedSession
                repository.updateChatSession(finalUpdatedSession)
            } catch (e: Exception) {
                Log.e("GutSyncViewModel", "Chat Error", e)
                _chatState.value = UiState.Error(e.localizedMessage ?: "Expert unavailable")
            }
        }
    }

    fun updateGoals(fiber: Int, polyphenols: Int, starch: Int) {
        val updatedProfile = appData.value.profile.copy(
            fiberGoal = fiber,
            polyphenolGoal = polyphenols,
            resistantStarchGoal = starch
        )
        viewModelScope.launch {
            repository.updateProfile(updatedProfile)
        }
    }

    fun signOut() {
        repository.clearData()
        _analysisState.value = UiState.Initial
        _chatState.value = UiState.Initial
        _analyzedFood.value = null
        _openRouterExplanation.value = null
        _capturedImage.value = null
        _chatImage.value = null
        _currentSession.value = ChatSession()
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
