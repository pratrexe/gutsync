package com.example.gutsync.data

import com.example.gutsync.BuildConfig
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import okhttp3.MediaType.Companion.toMediaType
import okhttp3.OkHttpClient
import okhttp3.Request
import okhttp3.RequestBody.Companion.toRequestBody
import org.json.JSONArray
import org.json.JSONObject

object GroqClient {
    private val API_KEY = BuildConfig.GROQ_API_KEY
    private const val BASE_URL = "https://api.groq.com/openai/v1/chat/completions"
    private val client = OkHttpClient()

    suspend fun generateContent(
        prompt: String, 
        model: String = "llama-3.3-70b-versatile", 
        isJson: Boolean = false,
        base64Image: String? = null
    ): String? = withContext(Dispatchers.IO) {
        try {
            val contentArray = JSONArray().apply {
                put(JSONObject().apply {
                    put("type", "text")
                    put("text", prompt)
                })
                base64Image?.let {
                    put(JSONObject().apply {
                        put("type", "image_url")
                        put("image_url", JSONObject().apply {
                            put("url", "data:image/jpeg;base64,$it")
                        })
                    })
                }
            }

            val json = JSONObject().apply {
                put("model", if (base64Image != null) "llama-3.2-11b-vision-preview" else model)
                put("messages", JSONArray().apply {
                    put(JSONObject().apply {
                        put("role", "user")
                        put("content", contentArray)
                    })
                })
                if (isJson) {
                    put("response_format", JSONObject().apply {
                        put("type", "json_object")
                    })
                }
            }

            val body = json.toString().toRequestBody("application/json".toMediaType())
            val request = Request.Builder()
                .url(BASE_URL)
                .addHeader("Authorization", "Bearer $API_KEY")
                .post(body)
                .build()

            client.newCall(request).execute().use { response ->
                if (!response.isSuccessful) {
                    val errorBody = response.body?.string()
                    throw Exception("Groq API Error ${response.code}: $errorBody")
                }
                val responseBody = response.body?.string()
                val jsonResponse = JSONObject(responseBody ?: "")
                jsonResponse.getJSONArray("choices")
                    .getJSONObject(0)
                    .getJSONObject("message")
                    .getString("content")
            }
        } catch (e: Exception) {
            e.printStackTrace()
            null
        }
    }
}
