package com.example.gutsync.data

import kotlinx.serialization.Serializable

enum class MicrobeType(val displayName: String) {
    BIFIDOBACTERIUM("Bifidobacterium"),
    LACTOBACILLUS("Lactobacillus"),
    AKKERMANSIA("Akkermansia"),
    BACTEROIDES("Bacteroides")
}

/**
 * Enhanced GIE Data Model (Step 1 & 2)
 */
@Serializable
data class NutrientData(
    val foodName: String = "",
    val calories: Int = 0,
    val fiber: Float = 0f, 
    val resistantStarch: Float = 0f, // Added for high-fidelity tracking
    val sugar: Float = 0f,
    val saturatedFats: Float = 0f,
    val animalProtein: Float = 0f,
    val polyphenols: Float = 0f,
    val fermentedStatus: Boolean = false,
    val artificialSweeteners: List<String> = emptyList(), // Negative impact on Bacteroides
    val additives: List<String> = emptyList(), // e.g., Emulsifiers
    val mainPrebioticCompound: String = "" // e.g., "Beta-Glucan"
)

@Serializable
data class GIEScorecard(
    val gutHealthScore: Int,
    val diversityScore: Int,
    val prebioticScore: Int,
    val probioticScore: Int,
    val inflammationRisk: Int,
    val predictedShifts: List<MicrobeShift>,
    val confidenceLevel: String, // "High", "Medium", "Low"
    val scientificReasoning: String
)

@Serializable
data class MicrobeShift(
    val microbeType: MicrobeType,
    val shiftPercentage: Float, // -100 to 100
    val confidence: Int // 0-100
)

object MicrobeImpactCalculator {
    /**
     * The GIE Logic (Step 3 - Local Implementation)
     * Maps Step 2 knowledge data into a weighted scoring model.
     */
    fun calculateGIE(nutrients: NutrientData): GIEScorecard {
        val shifts = mutableListOf<MicrobeShift>()
        
        // 1. Bifidobacterium & Lactobacillus: Boosted by Fiber, Starch, and Fermentation
        val fiberImpact = ((nutrients.fiber + nutrients.resistantStarch) * 5f).coerceAtMost(40f)
        val fermentedBonus = if (nutrients.fermentedStatus) 30f else 0f
        val bifidoLactoBase = (fiberImpact + fermentedBonus).coerceIn(0f, 100f)
        
        shifts.add(MicrobeShift(MicrobeType.BIFIDOBACTERIUM, bifidoLactoBase, 85))
        shifts.add(MicrobeShift(MicrobeType.LACTOBACILLUS, bifidoLactoBase, 80))

        // 2. Akkermansia: Boosted by Polyphenols
        val akkerImpact = (nutrients.polyphenols / 5f).coerceIn(0f, 100f)
        shifts.add(MicrobeShift(MicrobeType.AKKERMANSIA, akkerImpact, 70))

        // 3. Bacteroides: Inhibited by Sugar, Saturated Fats, and Additives
        var bacterPenalty = (nutrients.sugar * 2f) + (nutrients.saturatedFats * 3f)
        if (nutrients.additives.isNotEmpty()) bacterPenalty += 20f
        val bacterShift = -bacterPenalty.coerceIn(0f, 100f)
        shifts.add(MicrobeShift(MicrobeType.BACTEROIDES, bacterShift, 60))

        // 4. Synthesize Aggregate Scores
        val diversity = (bifidoLactoBase + akkerImpact).toInt().coerceIn(0, 100)
        val inflammation = bacterPenalty.toInt().coerceIn(0, 100)
        val overall = (diversity - (inflammation / 2) + 50).coerceIn(0, 100)

        return GIEScorecard(
            gutHealthScore = overall,
            diversityScore = diversity,
            prebioticScore = (nutrients.fiber * 3).toInt().coerceIn(0, 100),
            probioticScore = if (nutrients.fermentedStatus) 100 else 0,
            inflammationRisk = inflammation,
            predictedShifts = shifts,
            confidenceLevel = "High",
            scientificReasoning = "Based on presence of ${nutrients.mainPrebioticCompound ?: "fiber"} and nutrient profile."
        )
    }
}
