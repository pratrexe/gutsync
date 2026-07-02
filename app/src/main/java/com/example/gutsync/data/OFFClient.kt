package com.example.gutsync.data

import okhttp3.OkHttpClient
import okhttp3.Request
import org.json.JSONObject
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext

object OFFClient {
    private val client = OkHttpClient()
    private const val BASE_URL = "https://world.openfoodfacts.org/api/v2/product/"

    suspend fun getProductByBarcode(barcode: String): NutrientData? = withContext(Dispatchers.IO) {
        try {
            val request = Request.Builder()
                .url("$BASE_URL$barcode.json")
                .build()

            client.newCall(request).execute().use { response ->
                if (!response.isSuccessful) return@withContext null
                val body = response.body?.string() ?: return@withContext null
                val json = JSONObject(body)
                if (json.optInt("status") != 1) return@withContext null

                val product = json.getJSONObject("product")
                val nutrients = product.optJSONObject("nutriments")

                return@withContext NutrientData(
                    foodName = product.optString("product_name", "Unknown Product"),
                    calories = nutrients?.optDouble("energy-kcal_100g")?.toInt() ?: 0,
                    protein = nutrients?.optDouble("proteins_100g")?.toFloat() ?: 0f,
                    carbs = nutrients?.optDouble("carbohydrates_100g")?.toFloat() ?: 0f,
                    totalFat = nutrients?.optDouble("fat_100g")?.toFloat() ?: 0f,
                    fiber = nutrients?.optDouble("fiber_100g")?.toFloat() ?: 0f,
                    sugar = nutrients?.optDouble("sugars_100g")?.toFloat() ?: 0f,
                    saturatedFats = nutrients?.optDouble("saturated-fat_100g")?.toFloat() ?: 0f,
                    sourceFound = "Open Food Facts"
                )
            }
        } catch (e: Exception) {
            e.printStackTrace()
            null
        }
    }
}
