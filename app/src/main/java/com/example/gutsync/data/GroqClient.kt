package com.example.gutsync.data

import android.util.Log
import com.example.gutsync.BuildConfig
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.flow
import okhttp3.*
import okhttp3.MediaType.Companion.toMediaType
import okhttp3.RequestBody.Companion.toRequestBody
import org.json.JSONArray
import org.json.JSONObject
import java.io.IOException
import java.net.InetAddress
import java.net.InetSocketAddress
import java.net.Proxy

object GroqClient {
    private const val TAG = "GroqClient"
    private val GROQ_API_KEY = BuildConfig.GROQ_API_KEY
    private val OPENROUTER_API_KEY = BuildConfig.OPENROUTER_API_KEY
    private val NVIDIA_API_KEY = BuildConfig.NVIDIA_API_KEY

    private const val GROQ_URL = "https://api.groq.com/openai/v1/chat/completions"
    private const val OPENROUTER_URL = "https://openrouter.ai/api/v1/chat/completions"
    private const val NVIDIA_URL = "https://integrate.api.nvidia.com/v1/chat/completions"

    private val client = OkHttpClient.Builder()
        .connectTimeout(260, java.util.concurrent.TimeUnit.SECONDS)
        .readTimeout(260, java.util.concurrent.TimeUnit.SECONDS)
        .writeTimeout(260, java.util.concurrent.TimeUnit.SECONDS)
        .eventListener(object : EventListener() {
            // ... (keep existing logging)
            override fun callStart(call: Call) { Log.d(TAG, "CALL START: ${System.currentTimeMillis()}") }
            override fun dnsStart(call: Call, domainName: String) { Log.d(TAG, "DNS START: ${System.currentTimeMillis()}") }
            override fun dnsEnd(call: Call, domainName: String, inetAddressList: List<InetAddress>) { Log.d(TAG, "DNS END: ${System.currentTimeMillis()}") }
            override fun connectStart(call: Call, inetSocketAddress: InetSocketAddress, proxy: Proxy) { Log.d(TAG, "CONNECT START: ${System.currentTimeMillis()}") }
            override fun connectEnd(call: Call, inetSocketAddress: InetSocketAddress, proxy: Proxy, protocol: Protocol?) { Log.d(TAG, "CONNECT END: ${System.currentTimeMillis()}") }
            override fun responseHeadersStart(call: Call) { Log.d(TAG, "HEADERS START: ${System.currentTimeMillis()}") }
            override fun responseBodyStart(call: Call) { Log.d(TAG, "BODY START: ${System.currentTimeMillis()}") }
            override fun callEnd(call: Call) { Log.d(TAG, "CALL END: ${System.currentTimeMillis()}") }
            override fun callFailed(call: Call, ioe: IOException) { Log.e(TAG, "CALL FAILED: ${System.currentTimeMillis()}", ioe) }
        })
        .build()

    private val OPENROUTER_PREFIXES = listOf("google/", "meta-llama/", "mistralai/", "anthropic/")
    private val NVIDIA_PREFIXES = listOf("nvidia/")

    private fun isNvidiaModel(model: String) =
        NVIDIA_PREFIXES.any { model.startsWith(it) }

    private fun isOpenRouterModel(model: String) =
        OPENROUTER_PREFIXES.any { model.startsWith(it) }

    /**
     * Non-streaming version for backward compatibility (internal)
     */
    suspend fun generateContent(
        prompt: String,
        model: String = "llama-3.3-70b-versatile",
        isJson: Boolean = false,
        base64Image: String? = null
    ): String = withContext(Dispatchers.IO) {
        var result = ""
        generateContentStream(prompt, model, isJson, base64Image).collect {
            result += it
        }
        result
    }

    /**
     * Streaming version using Flow
     */
    fun generateContentStream(
        prompt: String,
        model: String = "llama-3.3-70b-versatile",
        isJson: Boolean = false,
        base64Image: String? = null
    ): Flow<String> = flow {
        val (url, key) = when {
            isNvidiaModel(model) -> NVIDIA_URL to NVIDIA_API_KEY
            isOpenRouterModel(model) -> OPENROUTER_URL to OPENROUTER_API_KEY
            else -> GROQ_URL to GROQ_API_KEY
        }

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
            put("model", model.removePrefix("nvidia/"))
            put("stream", true) // Enable streaming
            put("messages", JSONArray().apply {
                put(JSONObject().apply {
                    put("role", "user")
                    if (base64Image == null) {
                        put("content", prompt)
                    } else {
                        put("content", contentArray)
                    }
                })
            })

            // Model-specific optimized settings
            when {
                isNvidiaModel(model) -> {
                    put("max_tokens", 16384)
                    put("temperature", 1.00)
                    put("top_p", 0.95)
                    put("chat_template_kwargs", JSONObject().apply {
                        put("enable_thinking", true)
                    })
                }
                model == "meta-llama/llama-4-scout-17b-16e-instruct" -> {
                    put("max_completion_tokens", 1024)
                    put("temperature", 1.00)
                    put("top_p", 1.00)
                }
            }

            if (isJson && base64Image == null) {
                put("response_format", JSONObject().apply {
                    put("type", "json_object")
                })
            }
        }

        val body = json.toString().toRequestBody("application/json".toMediaType())
        val requestBuilder = Request.Builder()
            .url(url)
            .addHeader("Authorization", "Bearer $key")
            .addHeader("Content-Type", "application/json")
            .addHeader("Accept", "text/event-stream") // Accept SSE
            .post(body)

        if (url == OPENROUTER_URL) {
            requestBuilder.addHeader("HTTP-Referer", "https://gutsync.app")
            requestBuilder.addHeader("X-Title", "GutSync")
        }

        val response = client.newCall(requestBuilder.build()).execute()
        if (!response.isSuccessful) {
            val errorBody = response.body?.string()
            throw Exception("API Error ${response.code}: $errorBody")
        }

        val reader = response.body?.source()?.inputStream()?.bufferedReader()
        reader?.use { r ->
            var line: String?
            while (r.readLine().also { line = it } != null) {
                if (line?.startsWith("data: ") == true) {
                    val data = line?.substringAfter("data: ")?.trim()
                    if (data == "[DONE]") break
                    
                    try {
                        val jsonResp = JSONObject(data ?: "")
                        val content = jsonResp.getJSONArray("choices")
                            .getJSONObject(0)
                            .getJSONObject("delta")
                            .optString("content", "")
                        if (content.isNotEmpty()) {
                            emit(content)
                        }
                    } catch (e: Exception) {
                        // Skip malformed chunks
                    }
                }
            }
        }
    }
}
