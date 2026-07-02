package com.example.gutsync.ui.screens

import androidx.compose.animation.animateColorAsState
import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Info
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.gutsync.GutSyncViewModel
import com.example.gutsync.data.MicrobeImpactCalculator
import com.example.gutsync.data.NutrientData
import com.example.gutsync.ui.theme.SurfaceContainerLow
import com.example.gutsync.ui.theme.SurfaceContainerLowest
import androidx.lifecycle.viewmodel.compose.viewModel
import java.text.SimpleDateFormat
import java.util.*

@Composable
fun TrendsScreen(viewModel: GutSyncViewModel = viewModel()) {
    var selectedView by remember { mutableStateOf("Weekly") }
    val appData by viewModel.appData.collectAsState()
    
    val meals = appData.meals
    
    // Process Data for Graphs
    val graphData = remember(meals, selectedView) {
        val calendar = Calendar.getInstance()
        val now = calendar.timeInMillis
        
        if (selectedView == "Weekly") {
            // Weekly: Last 7 days
            (0 until 7).map { daysAgo ->
                val dayCalendar = Calendar.getInstance().apply {
                    add(Calendar.DAY_OF_YEAR, -daysAgo)
                    set(Calendar.HOUR_OF_DAY, 0)
                    set(Calendar.MINUTE, 0)
                    set(Calendar.SECOND, 0)
                    set(Calendar.MILLISECOND, 0)
                }
                val dayStart = dayCalendar.timeInMillis
                val dayEnd = dayStart + (24 * 60 * 60 * 1000)
                
                val dayMeals = meals.filter { it.timestamp in dayStart until dayEnd }
                val dayNutrients = NutrientData(
                    fiber = dayMeals.sumOf { it.nutrients.fiber.toDouble() }.toFloat(),
                    polyphenols = dayMeals.sumOf { it.nutrients.polyphenols.toDouble() }.toFloat(),
                    sugar = dayMeals.sumOf { it.nutrients.sugar.toDouble() }.toFloat(),
                    saturatedFats = dayMeals.sumOf { it.nutrients.saturatedFats.toDouble() }.toFloat(),
                    fermentedStatus = dayMeals.any { it.nutrients.fermentedStatus }
                )
                
                val scorecard = MicrobeImpactCalculator.calculateGIE(dayNutrients)
                val healthScore = if (dayMeals.isEmpty()) 0f else scorecard.gutHealthScore.toFloat()
                
                // Return data for the graph (reversed later)
                val dayLabel = when(daysAgo) {
                    0 -> "Today"
                    1 -> "Yesterday"
                    else -> SimpleDateFormat("E", Locale.getDefault()).format(dayCalendar.time)
                }
                dayLabel to (healthScore / 100f).coerceIn(0f, 1f)
            }.reversed()
        } else {
            // Monthly: Last 4 weeks (simplified as 30 days grouped by 5-day periods)
            (0 until 6).map { periodIndex ->
                val daysAgoStart = periodIndex * 5
                val daysAgoEnd = (periodIndex + 1) * 5
                
                val periodMeals = meals.filter { 
                    val mealDaysAgo = ((now - it.timestamp) / (24 * 60 * 60 * 1000)).toInt()
                    mealDaysAgo in daysAgoStart until daysAgoEnd
                }
                
                val periodScore = if (periodMeals.isEmpty()) 0f else {
                    periodMeals.map { MicrobeImpactCalculator.calculateGIE(it.nutrients).gutHealthScore }
                        .average().toFloat()
                }
                
                "P${6-periodIndex}" to (periodScore / 100f).coerceIn(0f, 1f)
            }.reversed()
        }
    }

    LazyColumn(
        modifier = Modifier
            .fillMaxSize()
            .padding(horizontal = 20.dp),
        verticalArrangement = Arrangement.spacedBy(24.dp)
    ) {
        item { Spacer(modifier = Modifier.height(16.dp)) }

        // Trends Toggle
        item {
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Text(text = "Trends", fontSize = 24.sp, fontWeight = FontWeight.SemiBold, color = Color.White)
                Surface(
                    color = SurfaceContainerLow,
                    border = androidx.compose.foundation.BorderStroke(1.dp, Color(0xFF303030)),
                    shape = CircleShape,
                    modifier = Modifier.padding(1.dp)
                ) {
                    Row(modifier = Modifier.padding(4.dp)) {
                        ToggleButton("Weekly", selectedView == "Weekly") { selectedView = "Weekly" }
                        ToggleButton("Monthly", selectedView == "Monthly") { selectedView = "Monthly" }
                    }
                }
            }
        }

        // Biological Insight Card
        item {
            val mostImpactedMicrobe = appData.meals.takeLast(20)
                .flatMap { MicrobeImpactCalculator.calculateGIE(it.nutrients).predictedShifts }
                .groupBy { it.microbeType }
                .mapValues { it.value.sumOf { shift -> shift.shiftPercentage.toDouble() } }
                .maxByOrNull { it.value }
            
            val insightText = if (mostImpactedMicrobe != null && mostImpactedMicrobe.value > 0) {
                "Your ${mostImpactedMicrobe.key.displayName} family is showing the strongest positive growth this period."
            } else if (meals.isEmpty()) {
                "Your gut health tracking is currently empty. Log your first meal to start calculating biological trends."
            } else {
                "Your gut microbiome is stabilizing. Maintain diverse fiber intake for optimal stability."
            }

            Card(
                colors = CardDefaults.cardColors(containerColor = SurfaceContainerLowest),
                border = androidx.compose.foundation.BorderStroke(1.dp, Color(0xFF303030)),
                shape = RoundedCornerShape(12.dp),
                modifier = Modifier.fillMaxWidth()
            ) {
                Column(modifier = Modifier.padding(16.dp), verticalArrangement = Arrangement.spacedBy(8.dp)) {
                    Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(4.dp)) {
                        Icon(Icons.Default.Info, contentDescription = null, tint = MaterialTheme.colorScheme.onSurfaceVariant, modifier = Modifier.size(16.dp))
                        Text(text = "BIOLOGICAL INSIGHT", fontSize = 10.sp, letterSpacing = 1.sp, color = MaterialTheme.colorScheme.onSurfaceVariant)
                    }
                    Text(
                        text = insightText,
                        fontSize = 18.sp,
                        color = Color.White,
                        lineHeight = 28.sp
                    )
                }
            }
        }

        // Visualization Card (REAL DATA GRAPH)
        item {
            Card(
                colors = CardDefaults.cardColors(containerColor = SurfaceContainerLowest),
                border = androidx.compose.foundation.BorderStroke(1.dp, Color(0xFF303030)),
                shape = RoundedCornerShape(12.dp),
                modifier = Modifier.fillMaxWidth()
            ) {
                Column(modifier = Modifier.padding(16.dp)) {
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.Bottom
                    ) {
                        Column {
                            Text(text = "Gut Health Score", fontSize = 12.sp, color = MaterialTheme.colorScheme.onSurfaceVariant)
                            Text(text = "$selectedView Progress", fontSize = 24.sp, fontWeight = FontWeight.SemiBold, color = Color.White)
                        }
                        LegendItem("Score", Color.White)
                    }
                    
                    Spacer(modifier = Modifier.height(32.dp))
                    
                    // Bar Chart
                    Row(
                        modifier = Modifier.fillMaxWidth().height(200.dp),
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.Bottom
                    ) {
                        graphData.forEach { (label, progress) ->
                            TrendBar(label, progress)
                        }
                    }
                }
            }
        }

        // Cumulative Stats
        item {
            val weekMillis = 7 * 24 * 60 * 60 * 1000L
            val weekMeals = meals.filter { it.timestamp > System.currentTimeMillis() - weekMillis }
            val avgFiber = if(weekMeals.isEmpty()) 0 else weekMeals.sumOf { it.nutrients.fiber.toInt() } / 7
            val avgPoly = if(weekMeals.isEmpty()) 0 else weekMeals.sumOf { it.nutrients.polyphenols.toInt() } / 7
            
            Row(horizontalArrangement = Arrangement.spacedBy(16.dp)) {
                StatCard("Daily Avg Fiber", "${avgFiber}g", (avgFiber / 35f).coerceAtMost(1f), Modifier.weight(1f))
                StatCard("Daily Avg Poly", "${avgPoly}mg", (avgPoly / 1000f).coerceAtMost(1f), Modifier.weight(1f))
            }
        }

        item { Spacer(modifier = Modifier.height(100.dp)) }
    }
}

@Composable
fun TrendBar(label: String, progress: Float) {
    val animatedProgress by animateFloatAsState(targetValue = progress)

    Column(horizontalAlignment = Alignment.CenterHorizontally, verticalArrangement = Arrangement.spacedBy(8.dp)) {
        Box(
            modifier = Modifier
                .width(24.dp)
                .fillMaxHeight(0.8f)
                .clip(RoundedCornerShape(topStart = 4.dp, topEnd = 4.dp))
                .background(Color.White.copy(alpha = 0.05f)),
            contentAlignment = Alignment.BottomCenter
        ) {
            Box(
                modifier = Modifier
                    .fillMaxWidth()
                    .fillMaxHeight(animatedProgress)
                    .background(Color.White)
            )
        }
        Text(
            text = label.take(3), 
            fontSize = 10.sp, 
            color = MaterialTheme.colorScheme.onSurfaceVariant,
            fontWeight = FontWeight.Medium
        )
    }
}

@Composable
fun ToggleButton(label: String, isSelected: Boolean, onClick: () -> Unit) {
    Button(
        onClick = onClick,
        colors = ButtonDefaults.buttonColors(
            containerColor = if (isSelected) Color.White else Color.Transparent,
            contentColor = if (isSelected) Color.Black else MaterialTheme.colorScheme.onSurfaceVariant
        ),
        shape = CircleShape,
        contentPadding = PaddingValues(horizontal = 16.dp, vertical = 6.dp),
        modifier = Modifier.height(32.dp)
    ) {
        Text(text = label, fontSize = 12.sp, fontWeight = FontWeight.SemiBold)
    }
}

@Composable
fun LegendItem(label: String, color: Color) {
    Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(4.dp)) {
        Box(modifier = Modifier.size(12.dp, 12.dp).background(color, RoundedCornerShape(2.dp)))
        Text(text = label, fontSize = 12.sp, color = MaterialTheme.colorScheme.onSurfaceVariant)
    }
}

@Composable
fun StatCard(label: String, value: String, progress: Float, modifier: Modifier = Modifier) {
    Card(
        colors = CardDefaults.cardColors(containerColor = SurfaceContainerLowest),
        border = androidx.compose.foundation.BorderStroke(1.dp, Color(0xFF303030)),
        shape = RoundedCornerShape(12.dp),
        modifier = modifier
    ) {
        Column(modifier = Modifier.padding(16.dp), verticalArrangement = Arrangement.spacedBy(4.dp)) {
            Text(text = label, fontSize = 12.sp, color = MaterialTheme.colorScheme.onSurfaceVariant)
            Text(text = value, fontSize = 20.sp, fontWeight = FontWeight.SemiBold, color = Color.White)
            Spacer(modifier = Modifier.height(4.dp))
            LinearProgressIndicator(
                progress = { progress },
                modifier = Modifier.fillMaxWidth().height(4.dp),
                color = Color.White,
                trackColor = Color(0xFF303030)
            )
        }
    }
}
