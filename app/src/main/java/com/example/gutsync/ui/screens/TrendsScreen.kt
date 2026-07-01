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
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.gutsync.GutSyncViewModel
import com.example.gutsync.data.MicrobeImpactCalculator
import com.example.gutsync.ui.theme.SurfaceContainerLow
import com.example.gutsync.ui.theme.SurfaceContainerLowest
import androidx.lifecycle.viewmodel.compose.viewModel

@Composable
fun TrendsScreen(viewModel: GutSyncViewModel = viewModel()) {
    var selectedView by remember { mutableStateOf("Weekly") }
    val appData by viewModel.appData.collectAsState()
    
    // Calculate real trends from meal history
    val recentMeals = appData.meals.takeLast(7)
    val trendData = recentMeals.map { log ->
        val scorecard = MicrobeImpactCalculator.calculateGIE(log.nutrients)
        val pro = (scorecard.gutHealthScore / 100f).coerceIn(0.1f, 0.9f)
        pro to (1f - pro)
    }.toMutableList()
    
    // Pad with placeholders if less than 7 meals
    while (trendData.size < 7) {
        trendData.add(0, 0.5f to 0.5f)
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

        // Biological Insight Card (REAL DATA)
        item {
            val mostImpactedMicrobe = appData.meals.takeLast(10)
                .flatMap { MicrobeImpactCalculator.calculateGIE(it.nutrients).predictedShifts }
                .groupBy { it.microbeType }
                .mapValues { it.value.sumOf { shift -> shift.shiftPercentage.toDouble() } }
                .maxByOrNull { it.value }
            
            val insightText = if (mostImpactedMicrobe != null && mostImpactedMicrobe.value > 0) {
                "Your ${mostImpactedMicrobe.key.displayName} levels are trending upward based on your recent logs."
            } else if (recentMeals.isEmpty()) {
                "Start logging your meals to see biological insights."
            } else {
                "Your gut microbiome is stabilizing across all core families."
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

        // Visualization Card
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
                            Text(text = "Metabolic State", fontSize = 12.sp, color = MaterialTheme.colorScheme.onSurfaceVariant)
                            Text(text = "Comparison", fontSize = 24.sp, fontWeight = FontWeight.SemiBold, color = Color.White)
                        }
                        Row(horizontalArrangement = Arrangement.spacedBy(12.dp)) {
                            LegendItem("Pro", Color.White)
                            LegendItem("Anti", Color(0xFF474747))
                        }
                    }
                    
                    Spacer(modifier = Modifier.height(32.dp))
                    
                    // Bar Chart
                    Row(
                        modifier = Modifier.fillMaxWidth().height(200.dp),
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.Bottom
                    ) {
                        val labels = if (selectedView == "Weekly") listOf("M", "T", "W", "T", "F", "S", "S") else listOf("W1", "W2", "W3", "W4", "W5", "W6", "W7")
                        
                        trendData.forEachIndexed { index, (pro, anti) ->
                            BarColumn(labels[index], pro, anti)
                        }
                    }
                }
            }
        }

        // Secondary Stats (REAL DATA)
        item {
            val totalFiber = recentMeals.sumOf { it.nutrients.fiber.toInt() }
            val totalPolyphenols = recentMeals.sumOf { it.nutrients.polyphenols.toInt() }
            Row(horizontalArrangement = Arrangement.spacedBy(16.dp)) {
                StatCard("Fiber Intake", "${totalFiber}g", (totalFiber / 35f).coerceAtMost(1f), Modifier.weight(1f))
                StatCard("Polyphenols", "${totalPolyphenols}mg", (totalPolyphenols / 1000f).coerceAtMost(1f), Modifier.weight(1f))
            }
        }

        item { Spacer(modifier = Modifier.height(100.dp)) }
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
fun BarColumn(label: String, pro: Float, anti: Float) {
    val animatedPro by animateFloatAsState(targetValue = pro)
    val animatedAnti by animateFloatAsState(targetValue = anti)

    Column(horizontalAlignment = Alignment.CenterHorizontally, verticalArrangement = Arrangement.spacedBy(8.dp)) {
        Row(modifier = Modifier.height(160.dp), verticalAlignment = Alignment.Bottom, horizontalArrangement = Arrangement.spacedBy(2.dp)) {
            Box(modifier = Modifier.width(12.dp).fillMaxHeight(animatedPro).background(Color.White))
            Box(modifier = Modifier.width(12.dp).fillMaxHeight(animatedAnti).background(Color(0xFF474747)))
        }
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
            Text(text = value, fontSize = 24.sp, fontWeight = FontWeight.SemiBold, color = Color.White)
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
