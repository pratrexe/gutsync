package com.example.gutsync.data.storage

import android.content.Context
import android.net.Uri
import com.example.gutsync.data.NutrientData
import java.io.BufferedReader
import java.io.InputStreamReader
import java.text.SimpleDateFormat
import java.util.*

object CsvHelper {
    private val dateFormatter = SimpleDateFormat("yyyy-MM-dd HH:mm:ss", Locale.getDefault())

    fun exportMealsToCsv(meals: List<MealLogEntry>): String {
        val header = "Timestamp,Food Name,Calories (kcal),Protein (g),Carbs (g),Total Fat (g),Fiber (g),Resistant Starch (g),Sugar (g),Saturated Fats (g),Animal Protein (g),Polyphenols (mg),Fermented,Additives,Source\n"
        val body = meals.joinToString("\n") { entry ->
            val n = entry.nutrients
            val timestamp = dateFormatter.format(Date(entry.timestamp))
            val additives = n.additives.joinToString(";")
            
            "\"$timestamp\",\"${n.foodName}\",${n.calories},${n.protein},${n.carbs},${n.totalFat},${n.fiber},${n.resistantStarch},${n.sugar},${n.saturatedFats},${n.animalProtein},${n.polyphenols},${n.fermentedStatus},\"$additives\",\"${n.sourceFound}\""
        }
        return header + body
    }

    fun importMealsFromCsv(context: Context, uri: Uri): List<MealLogEntry> {
        val meals = mutableListOf<MealLogEntry>()
        try {
            context.contentResolver.openInputStream(uri)?.use { inputStream ->
                BufferedReader(InputStreamReader(inputStream)).use { reader ->
                    // Skip header
                    reader.readLine()
                    
                    var line: String? = reader.readLine()
                    while (line != null) {
                        val parts = line.split(",(?=([^\"]*\"[^\"]*\")*[^\"]*$)".toRegex())
                        if (parts.size >= 12) {
                            try {
                                val timestampStr = parts[0].replace("\"", "")
                                val timestamp = dateFormatter.parse(timestampStr)?.time ?: System.currentTimeMillis()
                                
                                val foodName = parts[1].replace("\"", "")
                                val nutrients = NutrientData(
                                    foodName = foodName,
                                    calories = parts[2].toIntOrNull() ?: 0,
                                    protein = parts[3].toFloatOrNull() ?: 0f,
                                    carbs = parts[4].toFloatOrNull() ?: 0f,
                                    totalFat = parts[5].toFloatOrNull() ?: 0f,
                                    fiber = parts[6].toFloatOrNull() ?: 0f,
                                    resistantStarch = parts[7].toFloatOrNull() ?: 0f,
                                    sugar = parts[8].toFloatOrNull() ?: 0f,
                                    saturatedFats = parts[9].toFloatOrNull() ?: 0f,
                                    animalProtein = parts[10].toFloatOrNull() ?: 0f,
                                    polyphenols = parts[11].toFloatOrNull() ?: 0f,
                                    fermentedStatus = parts.getOrNull(12)?.toBoolean() ?: false,
                                    additives = parts.getOrNull(13)?.replace("\"", "")?.split(";")?.filter { it.isNotBlank() } ?: emptyList(),
                                    sourceFound = parts.getOrNull(14)?.replace("\"", "") ?: "CSV Import"
                                )
                                
                                meals.add(MealLogEntry(nutrients, timestamp))
                            } catch (e: Exception) {
                                e.printStackTrace()
                            }
                        }
                        line = reader.readLine()
                    }
                }
            }
        } catch (e: Exception) {
            e.printStackTrace()
        }
        return meals
    }
}
