package com.example.gutsync.ui.screens

import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.StrokeCap
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.gutsync.GutSyncViewModel
import com.example.gutsync.data.MicrobeType
import com.example.gutsync.data.MicrobeImpactCalculator
import com.example.gutsync.data.NutrientData
import com.example.gutsync.data.MicrobeShift
import com.example.gutsync.ui.theme.SurfaceContainerHighest
import com.example.gutsync.ui.theme.SurfaceContainerLowest
import androidx.lifecycle.viewmodel.compose.viewModel

@Composable
fun DashboardScreen(
    viewModel: GutSyncViewModel = viewModel()
) {
    val appData by viewModel.appData.collectAsState()
    val meals = appData.meals
    
    // Calculate REAL status based on meal history (Reset every day at midnight)
    val currentNutrients = if (meals.isNotEmpty()) {
        val calendar = java.util.Calendar.getInstance()
        calendar.set(java.util.Calendar.HOUR_OF_DAY, 0)
        calendar.set(java.util.Calendar.MINUTE, 0)
        calendar.set(java.util.Calendar.SECOND, 0)
        calendar.set(java.util.Calendar.MILLISECOND, 0)
        val startOfToday = calendar.timeInMillis
        
        val todayMeals = meals.filter { it.timestamp >= startOfToday }
        NutrientData(
            fiber = todayMeals.sumOf { it.nutrients.fiber.toDouble() }.toFloat(),
            resistantStarch = todayMeals.sumOf { it.nutrients.resistantStarch.toDouble() }.toFloat(),
            polyphenols = todayMeals.sumOf { it.nutrients.polyphenols.toDouble() }.toFloat(),
            sugar = todayMeals.sumOf { it.nutrients.sugar.toDouble() }.toFloat(),
            saturatedFats = todayMeals.sumOf { it.nutrients.saturatedFats.toDouble() }.toFloat(),
            fermentedStatus = todayMeals.any { it.nutrients.fermentedStatus }
        )
    } else NutrientData()

    val (healthScore, shifts) = remember(currentNutrients, meals) {
        val scorecard = MicrobeImpactCalculator.calculateGIE(currentNutrients)
        // If we have no meals today, show a starting "resting" diversity rather than 0
        val finalScore = if (currentNutrients.fiber == 0f && currentNutrients.polyphenols == 0f) {
            42 // Baseline diversity when no food is logged yet today
        } else {
            scorecard.gutHealthScore
        }
        finalScore to scorecard.predictedShifts
    }

    // Calculate REAL growth percentage (Immediate feedback: current score vs baseline)
    val growthPercentage = remember(healthScore) {
        val baseline = 42.0
        if (healthScore > baseline) {
            (((healthScore - baseline) / baseline) * 100).toInt()
        } else {
            0
        }
    }

    LazyColumn(
        modifier = Modifier
            .fillMaxSize()
            .padding(horizontal = 20.dp),
        horizontalAlignment = Alignment.CenterHorizontally,
        verticalArrangement = Arrangement.spacedBy(24.dp)
    ) {
        item { Spacer(modifier = Modifier.height(16.dp)) }

        item {
            ScoreHeroSection(score = healthScore, growth = growthPercentage)
        }

        // Fiber Goal Card
        item {
            GoalCard(label = "Fiber Intake", current = currentNutrients.fiber.toInt(), goal = appData.profile.fiberGoal)
        }

        // Starch & Polyphenol Progress
        item {
            Row(horizontalArrangement = Arrangement.spacedBy(16.dp)) {
                Box(modifier = Modifier.weight(1f)) {
                    MiniGoalCard("Starch", currentNutrients.resistantStarch.toInt(), appData.profile.resistantStarchGoal)
                }
                Box(modifier = Modifier.weight(1f)) {
                    MiniGoalCard("Polyphenols", currentNutrients.polyphenols.toInt(), appData.profile.polyphenolGoal)
                }
            }
        }

        // Microbe Status Grid (REAL DATA)
        item {
            MicrobeStatusGrid(shifts)
        }

        // Insight Card
        item {
            val insight = when {
                healthScore < 50 -> "Pro-inflammatory" to "Your gut diversity is low. Focus on increasing prebiotic fiber and reducing refined sugars."
                healthScore < 80 -> "Building Stability" to "Good progress! Your microbiome is stabilizing. Add more fermented foods to boost Lactobacillus."
                else -> "Optimal Diversity" to "Excellent! Your dietary patterns are promoting a highly diverse and stable microbial environment."
            }
            InsightCard(title = "Focus: ${insight.first}", description = insight.second)
        }

        item { Spacer(modifier = Modifier.height(100.dp)) }
    }
}

@Composable
fun MicrobeStatusGrid(shifts: List<MicrobeShift>) {
    Column(verticalArrangement = Arrangement.spacedBy(16.dp)) {
        Row(horizontalArrangement = Arrangement.spacedBy(16.dp)) {
            val bifido = (30 + (shifts.find { it.microbeType == MicrobeType.BIFIDOBACTERIUM }?.shiftPercentage?.toInt() ?: 0)).coerceIn(10, 100)
            val lacto = (35 + (shifts.find { it.microbeType == MicrobeType.LACTOBACILLUS }?.shiftPercentage?.toInt() ?: 0)).coerceIn(10, 100)
            MicrobeStatusCard(MicrobeType.BIFIDOBACTERIUM, bifido, getStatusText(bifido), Modifier.weight(1f))
            MicrobeStatusCard(MicrobeType.LACTOBACILLUS, lacto, getStatusText(lacto), Modifier.weight(1f))
        }
        Row(horizontalArrangement = Arrangement.spacedBy(16.dp)) {
            val akker = (25 + (shifts.find { it.microbeType == MicrobeType.AKKERMANSIA }?.shiftPercentage?.toInt() ?: 0)).coerceIn(10, 100)
            val bacter = (40 + (shifts.find { it.microbeType == MicrobeType.BACTEROIDES }?.shiftPercentage?.toInt() ?: 0)).coerceIn(10, 100)
            MicrobeStatusCard(MicrobeType.AKKERMANSIA, akker, getStatusText(akker), Modifier.weight(1f))
            MicrobeStatusCard(MicrobeType.BACTEROIDES, bacter, getStatusText(bacter), Modifier.weight(1f))
        }
    }
}

fun getStatusText(percentage: Int) = when {
    percentage < 30 -> "Low"
    percentage < 70 -> "Moderate"
    else -> "Optimal"
}

@Composable
fun MiniGoalCard(label: String, current: Int, goal: Int) {
    Card(
        colors = CardDefaults.cardColors(containerColor = SurfaceContainerLowest),
        border = BorderStroke(1.dp, MaterialTheme.colorScheme.outlineVariant),
        shape = RoundedCornerShape(12.dp),
        modifier = Modifier.fillMaxWidth()
    ) {
        Column(modifier = Modifier.padding(16.dp)) {
            Text(label.uppercase(), fontSize = 10.sp, color = MaterialTheme.colorScheme.onSurfaceVariant)
            Text("${current} / ${goal}${if(label == "Polyphenols") "mg" else "g"}", fontSize = 14.sp, fontWeight = FontWeight.Bold, color = Color.White)
            Spacer(modifier = Modifier.height(8.dp))
            LinearProgressIndicator(
                progress = { if (goal > 0) current.toFloat() / goal else 0f },
                modifier = Modifier.fillMaxWidth().height(4.dp),
                color = Color.White,
                trackColor = SurfaceContainerHighest,
                strokeCap = StrokeCap.Round
            )
        }
    }
}

@Composable
fun ScoreHeroSection(score: Int, growth: Int) {
    Column(
        horizontalAlignment = Alignment.CenterHorizontally,
        modifier = Modifier.padding(vertical = 48.dp)
    ) {
        Box(contentAlignment = Alignment.Center, modifier = Modifier.size(200.dp)) {
            Canvas(modifier = Modifier.size(180.dp)) {
                drawCircle(
                    color = SurfaceContainerHighest,
                    style = Stroke(width = 4.dp.toPx())
                )
                drawArc(
                    color = Color.White,
                    startAngle = -90f,
                    sweepAngle = (score / 100f) * 360f,
                    useCenter = false,
                    style = Stroke(width = 4.dp.toPx(), cap = StrokeCap.Round)
                )
            }
            Column(horizontalAlignment = Alignment.CenterHorizontally) {
                Text(
                    text = score.toString(),
                    fontSize = 48.sp,
                    fontWeight = FontWeight.Bold,
                    color = Color.White
                )
                Text(
                    text = "HEALTH SCORE",
                    fontSize = 12.sp,
                    letterSpacing = 2.sp,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
            }
        }
        Text(
            text = "Your gut diversity has increased by $growth% since your last upload.",
            fontSize = 16.sp,
            color = MaterialTheme.colorScheme.onSurfaceVariant,
            textAlign = TextAlign.Center,
            modifier = Modifier.padding(top = 24.dp).width(250.dp)
        )
    }
}

@Composable
fun GoalCard(label: String, current: Int, goal: Int) {
    Card(
        colors = CardDefaults.cardColors(containerColor = SurfaceContainerLowest),
        border = BorderStroke(1.dp, MaterialTheme.colorScheme.outlineVariant),
        shape = RoundedCornerShape(12.dp),
        modifier = Modifier.fillMaxWidth()
    ) {
        Row(
            modifier = Modifier.padding(24.dp),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.SpaceBetween
        ) {
            Column {
                Text(
                    text = label.uppercase(),
                    fontSize = 12.sp,
                    letterSpacing = 1.sp,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
                Row(verticalAlignment = Alignment.Bottom) {
                    Text(
                        text = "${current}g",
                        fontSize = 24.sp,
                        fontWeight = FontWeight.Bold,
                        color = Color.White
                    )
                    Text(
                        text = " / ${goal}g goal",
                        fontSize = 16.sp,
                        color = MaterialTheme.colorScheme.onSurfaceVariant,
                        modifier = Modifier.padding(start = 8.dp, bottom = 2.dp)
                    )
                }
            }
            Box(
                modifier = Modifier
                    .width(100.dp)
                    .height(8.dp)
                    .background(SurfaceContainerHighest, CircleShape)
            ) {
                Box(
                    modifier = Modifier
                        .fillMaxWidth(if (goal > 0) current.toFloat() / goal else 0f)
                        .fillMaxHeight()
                        .background(Color.White, CircleShape)
                )
            }
        }
    }
}

@Composable
fun MicrobeStatusCard(type: MicrobeType, percentage: Int, status: String, modifier: Modifier = Modifier) {
    Card(
        colors = CardDefaults.cardColors(containerColor = SurfaceContainerLowest),
        border = BorderStroke(1.dp, MaterialTheme.colorScheme.outlineVariant),
        shape = RoundedCornerShape(12.dp),
        modifier = modifier
    ) {
        Column(modifier = Modifier.padding(16.dp)) {
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.Top
            ) {
                Text(
                    text = type.displayName,
                    fontSize = 14.sp,
                    fontWeight = FontWeight.Bold,
                    color = Color.White,
                    modifier = Modifier.weight(1f)
                )
                Surface(
                    color = if (status == "Low") SurfaceContainerHighest else MaterialTheme.colorScheme.surfaceVariant,
                    shape = CircleShape,
                    modifier = Modifier.padding(start = 4.dp)
                ) {
                    Text(
                        text = status,
                        fontSize = 10.sp,
                        color = if (status == "Low") Color.White else MaterialTheme.colorScheme.onSurfaceVariant,
                        modifier = Modifier.padding(horizontal = 10.dp, vertical = 4.dp),
                        maxLines = 1
                    )
                }
            }
            Spacer(modifier = Modifier.height(16.dp))
            LinearProgressIndicator(
                progress = { percentage / 100f },
                modifier = Modifier.fillMaxWidth().height(4.dp),
                color = if (status == "Low") MaterialTheme.colorScheme.secondary else Color.White,
                trackColor = SurfaceContainerHighest,
                strokeCap = StrokeCap.Round
            )
            Text(
                text = "$percentage%",
                fontSize = 12.sp,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
                modifier = Modifier.fillMaxWidth().padding(top = 4.dp),
                textAlign = TextAlign.End
            )
        }
    }
}

@Composable
fun InsightCard(title: String, description: String) {
    Card(
        colors = CardDefaults.cardColors(containerColor = Color.White),
        shape = RoundedCornerShape(12.dp),
        modifier = Modifier.fillMaxWidth()
    ) {
        Column(modifier = Modifier.padding(24.dp)) {
            Text(
                text = title,
                fontSize = 24.sp,
                fontWeight = FontWeight.SemiBold,
                color = Color.Black
            )
            Text(
                text = description,
                fontSize = 16.sp,
                color = Color(0xFF52525B),
                lineHeight = 24.sp,
                modifier = Modifier.padding(vertical = 16.dp)
            )
            Button(
                onClick = {},
                colors = ButtonDefaults.buttonColors(containerColor = Color.Black),
                shape = CircleShape,
                contentPadding = PaddingValues(horizontal = 24.dp, vertical = 12.dp)
            ) {
                Text(text = "View Diet Plan", color = Color.White, fontSize = 14.sp)
            }
        }
    }
}
