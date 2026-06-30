package com.example.gutsync.data

enum class MicrobeType(val displayName: String) {
    BIFIDOBACTERIUM("Bifidobacterium"),
    LACTOBACILLUS("Lactobacillus"),
    AKKERMANSIA("Akkermansia"),
    BACTEROIDES("Bacteroides")
}

data class NutrientData(
    val fiber: Float = 0f, // in grams
    val saturatedFats: Float = 0f, // in grams
    val refinedSugars: Float = 0f, // in grams
    val animalProtein: Float = 0f, // in grams
    val polyphenols: Float = 0f, // in mg
    val fermentedCultures: Int = 0 // count of active cultures
)

data class MicrobeShift(
    val microbeType: MicrobeType,
    val shiftPercentage: Float // -100 to 100
)

object MicrobeImpactCalculator {
    /**
     * Calculates the impact of a meal on the 4 core microbial families.
     * Returns a score from 0 to 100 and a list of shifts.
     */
    fun calculateImpact(nutrients: NutrientData): Pair<Int, List<MicrobeShift>> {
        val shifts = mutableListOf<MicrobeShift>()
        
        // Bifidobacterium & Lactobacillus: Promoted by Fiber and Fermented Foods
        val fiberBonus = (nutrients.fiber / 10f).coerceAtMost(0.4f)
        val fermentedBonus = (nutrients.fermentedCultures * 0.1f).coerceAtMost(0.3f)
        
        shifts.add(MicrobeShift(MicrobeType.BIFIDOBACTERIUM, (fiberBonus + fermentedBonus) * 100))
        shifts.add(MicrobeShift(MicrobeType.LACTOBACILLUS, (fiberBonus + fermentedBonus) * 100))

        // Akkermansia: Promoted by Polyphenols
        val polyphenolBonus = (nutrients.polyphenols / 500f).coerceAtMost(0.5f)
        shifts.add(MicrobeShift(MicrobeType.AKKERMANSIA, polyphenolBonus * 100))

        // Bacteroides: Inhibited by Saturated Fats & Sugars
        val fatPenalty = (nutrients.saturatedFats / 20f).coerceAtMost(0.3f)
        val sugarPenalty = (nutrients.refinedSugars / 30f).coerceAtMost(0.3f)
        shifts.add(MicrobeShift(MicrobeType.BACTEROIDES, -(fatPenalty + sugarPenalty) * 100))

        // Final Score calculation (base 70, adjusted by nutrients)
        var score = 70f
        score += (nutrients.fiber * 2f)
        score += (nutrients.polyphenols / 50f)
        score -= (nutrients.saturatedFats * 1.5f)
        score -= (nutrients.refinedSugars * 1.5f)
        
        return Pair(score.toInt().coerceIn(0, 100), shifts)
    }
}
