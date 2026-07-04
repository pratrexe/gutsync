package com.example.gutsync.ui.screens

import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.animation.core.animateIntAsState
import androidx.compose.animation.core.tween
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
import androidx.compose.ui.draw.clip
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.PathEffect
import androidx.compose.ui.graphics.StrokeCap
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.lifecycle.viewmodel.compose.viewModel
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Eco
import androidx.compose.material.icons.filled.Grain
import androidx.compose.material.icons.filled.Opacity
import com.example.gutsync.GutSyncViewModel
import com.example.gutsync.data.MicrobeType
import com.example.gutsync.data.MicrobeImpactCalculator
import com.example.gutsync.data.NutrientData
import com.example.gutsync.data.MicrobeShift
import com.example.gutsync.ui.theme.SurfaceContainerHighest
import com.example.gutsync.ui.theme.SurfaceContainerLowest

@Composable
fun DashboardScreen(
    viewModel: GutSyncViewModel = viewModel()
) {
    val appData by viewModel.appData.collectAsState()
    val meals = appData.meals

    val currentNutrients = if (meals.isNotEmpty()) {
        val calendar = java.util.Calendar.getInstance()
        calendar.set(java.util.Calendar.HOUR_OF_DAY, 0)
        calendar.set(java.util.Calendar.MINUTE, 0)
        calendar.set(java.util.Calendar.SECOND, 0)
        calendar.set(java.util.Calendar.MILLISECOND, 0)
        val startOfToday = calendar.timeInMillis

        val todayMeals = meals.filter { it.timestamp >= startOfToday }
        NutrientData(
            foodName = "Today's Intake",
            calories = todayMeals.sumOf { it.nutrients.calories },
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
        val finalScore = if (currentNutrients.fiber == 0f && currentNutrients.polyphenols == 0f) 0 else scorecard.gutHealthScore
        finalScore to scorecard.predictedShifts
    }

    LazyColumn(
        modifier = Modifier
            .fillMaxSize()
            .padding(horizontal = 20.dp),
        horizontalAlignment = Alignment.CenterHorizontally,
        verticalArrangement = Arrangement.spacedBy(20.dp)
    ) {
        item { Spacer(modifier = Modifier.height(16.dp)) }

        item {
            ScoreHeroSection(score = healthScore)
        }

        item {
            Column(verticalArrangement = Arrangement.spacedBy(16.dp)) {
                MetricCard(
                    icon = Icons.Default.Eco,
                    value = "${currentNutrients.fiber.toInt()}",
                    unit = "g",
                    label = "Fiber Intake",
                    subtitle = "Goal: ${appData.profile.fiberGoal}g",
                    progress = if (appData.profile.fiberGoal > 0) currentNutrients.fiber / appData.profile.fiberGoal else 0f,
                    color = Color(0xFF4CAF50),
                    modifier = Modifier.fillMaxWidth(),
                    isSlim = true
                )

                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.spacedBy(16.dp)
                ) {
                    MetricCard(
                        icon = Icons.Default.Grain,
                        value = "${currentNutrients.resistantStarch.toInt()}",
                        unit = "g",
                        label = "Starch",
                        subtitle = "Goal: ${appData.profile.resistantStarchGoal}g",
                        color = Color(0xFFFFC107),
                        modifier = Modifier.weight(1f)
                    )
                    MetricCard(
                        icon = Icons.Default.Opacity,
                        value = "${currentNutrients.polyphenols.toInt()}",
                        unit = "mg",
                        label = "Polyphenols",
                        subtitle = "Goal: ${appData.profile.polyphenolGoal}mg",
                        color = Color(0xFF9C27B0),
                        modifier = Modifier.weight(1f)
                    )
                }
            }
        }

        item {
            MicrobeStatusGrid(shifts)
        }

        item {
            val insight = when {
                healthScore < 0 -> "High Inflammation" to "Your gut is in a pro-inflammatory state. Avoid processed sugars and fats immediately."
                healthScore < 50 -> "Pro-inflammatory" to "Your gut diversity is low. Focus on increasing prebiotic fiber and reducing refined sugars."
                healthScore < 80 -> "Building Stability" to "Good progress! Your microbiome is stabilizing. Add more fermented foods to boost Lactobacillus."
                else -> "Optimal Diversity" to "Excellent! Your dietary patterns are promoting a highly diverse and stable microbial environment."
            }

            var showPlanDialog by remember { mutableStateOf(false) }
            val dietPlan = appData.dietPlan

            InsightCard(
                title = "Focus: ${insight.first}",
                description = insight.second,
                hasPlan = dietPlan != null,
                onAction = {
                    if (dietPlan != null) {
                        showPlanDialog = true
                    } else {
                        viewModel.generateAiDietPlan()
                    }
                }
            )

            if (showPlanDialog && dietPlan != null) {
                AlertDialog(
                    onDismissRequest = { showPlanDialog = false },
                    title = { Text("AI Diet Plan") },
                    text = {
                        Box(modifier = Modifier.heightIn(max = 400.dp)) {
                            LazyColumn {
                                item { Text(dietPlan) }
                            }
                        }
                    },
                    confirmButton = {
                        TextButton(onClick = { showPlanDialog = false }) {
                            Text("Close")
                        }
                    },
                    containerColor = SurfaceContainerLowest,
                    titleContentColor = Color.White,
                    textContentColor = Color.White
                )
            }
        }

        item { Spacer(modifier = Modifier.height(100.dp)) }
    }
}

@Composable
fun MetricCard(
    icon: ImageVector,
    value: String,
    unit: String,
    label: String,
    subtitle: String,
    color: Color,
    modifier: Modifier = Modifier,
    progress: Float? = null,
    isSlim: Boolean = false
) {
    Card(
        colors = CardDefaults.cardColors(containerColor = Color(0xFF1C1C1E)),
        shape = RoundedCornerShape(24.dp),
        border = BorderStroke(1.dp, Color.White.copy(alpha = 0.05f)),
        modifier = modifier
    ) {
        if (isSlim) {
            Column(
                modifier = Modifier.padding(20.dp),
                verticalArrangement = Arrangement.spacedBy(16.dp)
            ) {
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.spacedBy(16.dp)
                ) {
                    Box(
                        modifier = Modifier
                            .size(48.dp)
                            .clip(RoundedCornerShape(12.dp))
                            .background(color.copy(alpha = 0.1f)),
                        contentAlignment = Alignment.Center
                    ) {
                        Icon(
                            imageVector = icon,
                            contentDescription = null,
                            tint = color,
                            modifier = Modifier.size(24.dp)
                        )
                    }

                    Column(modifier = Modifier.weight(1f)) {
                        Row(verticalAlignment = Alignment.Bottom, horizontalArrangement = Arrangement.spacedBy(4.dp)) {
                            Text(
                                text = value,
                                fontSize = 32.sp,
                                fontWeight = FontWeight.Bold,
                                color = Color.White
                            )
                            Text(
                                text = unit,
                                fontSize = 16.sp,
                                color = MaterialTheme.colorScheme.onSurfaceVariant,
                                modifier = Modifier.padding(bottom = 4.dp)
                            )
                        }
                        Text(
                            text = label,
                            fontSize = 14.sp,
                            fontWeight = FontWeight.Medium,
                            color = Color.White.copy(alpha = 0.7f)
                        )
                    }

                    Text(
                        text = subtitle,
                        fontSize = 12.sp,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                }

                if (progress != null) {
                    LinearProgressIndicator(
                        progress = { progress.coerceIn(0f, 1f) },
                        modifier = Modifier.fillMaxWidth().height(4.dp),
                        color = color,
                        trackColor = color.copy(alpha = 0.1f),
                        strokeCap = StrokeCap.Round
                    )
                }
            }
        } else {
            Column(
                modifier = Modifier.padding(20.dp),
                verticalArrangement = Arrangement.spacedBy(16.dp)
            ) {
                Box(
                    modifier = Modifier
                        .size(48.dp)
                        .clip(RoundedCornerShape(12.dp))
                        .background(color.copy(alpha = 0.1f)),
                    contentAlignment = Alignment.Center
                ) {
                    Icon(
                        imageVector = icon,
                        contentDescription = null,
                        tint = color,
                        modifier = Modifier.size(24.dp)
                    )
                }

                Column {
                    Row(verticalAlignment = Alignment.Bottom, horizontalArrangement = Arrangement.spacedBy(4.dp)) {
                        Text(
                            text = value,
                            fontSize = 32.sp,
                            fontWeight = FontWeight.Bold,
                            color = Color.White
                        )
                        Text(
                            text = unit,
                            fontSize = 16.sp,
                            color = MaterialTheme.colorScheme.onSurfaceVariant,
                            modifier = Modifier.padding(bottom = 4.dp)
                        )
                    }
                    Text(
                        text = label,
                        fontSize = 14.sp,
                        fontWeight = FontWeight.Medium,
                        color = Color.White.copy(alpha = 0.7f)
                    )
                }

                if (progress != null) {
                    LinearProgressIndicator(
                        progress = { progress.coerceIn(0f, 1f) },
                        modifier = Modifier.fillMaxWidth().height(4.dp),
                        color = color,
                        trackColor = color.copy(alpha = 0.1f),
                        strokeCap = StrokeCap.Round
                    )
                }

                Text(
                    text = subtitle,
                    fontSize = 12.sp,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
            }
        }
    }
}

@Composable
fun MicrobeStatusGrid(shifts: List<MicrobeShift>) {
    Column(verticalArrangement = Arrangement.spacedBy(16.dp)) {
        Row(horizontalArrangement = Arrangement.spacedBy(16.dp)) {
            val bifido = shifts.find { it.microbeType == MicrobeType.BIFIDOBACTERIUM }?.shiftPercentage?.toInt() ?: 0
            val lacto = shifts.find { it.microbeType == MicrobeType.LACTOBACILLUS }?.shiftPercentage?.toInt() ?: 0
            
            MetricCard(
                icon = Icons.Default.Eco, // Placeholder, usually these need specific icons
                value = "$bifido%",
                unit = "",
                label = "Bifidobacterium",
                subtitle = getStatusText(bifido),
                progress = bifido / 100f,
                color = Color(0xFF4CAF50),
                modifier = Modifier.weight(1f)
            )
            MetricCard(
                icon = Icons.Default.Eco,
                value = "$lacto%",
                unit = "",
                label = "Lactobacillus",
                subtitle = getStatusText(lacto),
                progress = lacto / 100f,
                color = Color(0xFF8BC34A),
                modifier = Modifier.weight(1f)
            )
        }
        Row(horizontalArrangement = Arrangement.spacedBy(16.dp)) {
            val akker = shifts.find { it.microbeType == MicrobeType.AKKERMANSIA }?.shiftPercentage?.toInt() ?: 0
            val bacter = shifts.find { it.microbeType == MicrobeType.BACTEROIDES }?.shiftPercentage?.toInt() ?: 0
            
            MetricCard(
                icon = Icons.Default.Eco,
                value = "$akker%",
                unit = "",
                label = "Akkermansia",
                subtitle = getStatusText(akker),
                progress = akker / 100f,
                color = Color(0xFFCDDC39),
                modifier = Modifier.weight(1f)
            )
            MetricCard(
                icon = Icons.Default.Eco,
                value = "$bacter%",
                unit = "",
                label = "Bacteroides",
                subtitle = getStatusText(bacter),
                progress = bacter / 100f,
                color = Color(0xFFFFEB3B),
                modifier = Modifier.weight(1f)
            )
        }
    }
}

fun getStatusText(percentage: Int) = when {
    percentage < 30 -> "Low"
    percentage < 70 -> "Moderate"
    else -> "Optimal"
}

@Composable
fun ScoreHeroSection(score: Int) {
    val progress = (score.coerceIn(0, 100) / 100f)
    val animatedScore by animateIntAsState(targetValue = score, animationSpec = tween(1500), label = "score")
    val animatedProgress by animateFloatAsState(targetValue = progress, animationSpec = tween(1500), label = "progress")

    Column(
        modifier = Modifier
            .fillMaxWidth()
            .padding(vertical = 16.dp)
    ) {
        // Gut Health Label
        Text(
            text = "Gut Health",
            fontSize = 14.sp,
            fontWeight = FontWeight.Medium,
            color = Color.White.copy(alpha = 0.6f),
            modifier = Modifier.padding(bottom = 4.dp)
        )

        // Score Header
        Row(
            verticalAlignment = Alignment.Bottom,
            modifier = Modifier.padding(bottom = 24.dp)
        ) {
            Text(
                text = animatedScore.toString(),
                fontSize = 56.sp,
                fontWeight = FontWeight.Medium,
                color = Color.White,
                lineHeight = 56.sp
            )
            Text(
                text = " /100",
                fontSize = 24.sp,
                fontWeight = FontWeight.Normal,
                color = Color.White.copy(alpha = 0.4f),
                modifier = Modifier.padding(bottom = 6.dp, start = 4.dp)
            )
        }

        // Arc Gauge Layout
        Box(
            modifier = Modifier
                .fillMaxWidth()
                .height(210.dp) // Adjusted from 220.dp to shift gauge up
                .offset(y = (-60).dp), // Moves the gauge 10px up
            contentAlignment = Alignment.TopCenter
        ) {
            Canvas(modifier = Modifier.fillMaxSize()) {
                val strokeWidth = 28.dp.toPx()
                // Ensure sufficient padding so shadows and the line don't clip
                val padding = strokeWidth / 2 + 16.dp.toPx()
                val arcDiameter = size.width - (padding * 2)

                val topLeftOffset = Offset(padding, padding)
                val arcSize = Size(arcDiameter, arcDiameter)
                val radius = arcDiameter / 2
                val center = Offset(padding + radius, padding + radius)

                // Define arc constraints (sweeping 240 degrees over the top)
                val startAngle = 150f
                val sweepAngle = 240f
                val currentAngle = startAngle + (sweepAngle * animatedProgress)

                val dotColor = Color(0xFF52D385)
                val trackBaseColor = Color(0xFF2A2A2A)
                val trackStripeColor = Color(0xFF1A1A1A)

                // 1. Draw Solid Base Track
                drawArc(
                    color = trackBaseColor,
                    startAngle = startAngle,
                    sweepAngle = sweepAngle,
                    useCenter = false,
                    style = Stroke(width = strokeWidth, cap = StrokeCap.Butt),
                    topLeft = topLeftOffset,
                    size = arcSize
                )

                // 2. Draw Subtle Ridges Overlay (simulating the hatched pattern)
                drawArc(
                    color = trackStripeColor,
                    startAngle = startAngle,
                    sweepAngle = sweepAngle,
                    useCenter = false,
                    style = Stroke(
                        width = strokeWidth,
                        cap = StrokeCap.Butt,
                        pathEffect = PathEffect.dashPathEffect(floatArrayOf(6.dp.toPx(), 4.dp.toPx()))
                    ),
                    topLeft = topLeftOffset,
                    size = arcSize
                )

                // 3. Draw Green Progress Arc
                drawArc(
                    color = dotColor,
                    startAngle = startAngle,
                    sweepAngle = sweepAngle * animatedProgress,
                    useCenter = false,
                    style = Stroke(width = strokeWidth, cap = StrokeCap.Butt),
                    topLeft = topLeftOffset,
                    size = arcSize
                )

                // 4. Calculate exact cap positions
                val startAngleRad = Math.toRadians(startAngle.toDouble())
                val startX = center.x + radius * Math.cos(startAngleRad).toFloat()
                val startY = center.y + radius * Math.sin(startAngleRad).toFloat()

                val currentAngleRad = Math.toRadians(currentAngle.toDouble())
                val thumbX = center.x + radius * Math.cos(currentAngleRad).toFloat()
                val thumbY = center.y + radius * Math.sin(currentAngleRad).toFloat()

                // 5. Left Cap (Starting point)
                drawCircle(
                    color = Color(0xFF303030),
                    radius = strokeWidth / 2,
                    center = Offset(startX, startY)
                )
                drawCircle(
                    color = Color(0xFF121212),
                    radius = strokeWidth / 4,
                    center = Offset(startX, startY)
                )

                // 6. Right Cap (Glowing Thumb)
                // Simulated Box Shadow Gradient
                drawCircle(
                    brush = Brush.radialGradient(
                        colors = listOf(Color.Black.copy(alpha = 0.8f), Color.Transparent),
                        center = Offset(thumbX, thumbY + 8f),
                        radius = strokeWidth
                    ),
                    center = Offset(thumbX, thumbY + 8f),
                    radius = strokeWidth
                )
                // Outer Green Thumb
                drawCircle(
                    color = dotColor,
                    radius = strokeWidth / 2,
                    center = Offset(thumbX, thumbY)
                )
                // Inner White Dot
                drawCircle(
                    color = Color.White,
                    radius = strokeWidth / 4.5f,
                    center = Offset(thumbX, thumbY)
                )

                // 7. Vertical Fading Drop Line
                drawLine(
                    brush = Brush.verticalGradient(
                        colors = listOf(Color.White.copy(alpha = 0.5f), Color.Transparent),
                        startY = thumbY,
                        endY = thumbY + 80.dp.toPx()
                    ),
                    start = Offset(thumbX, thumbY),
                    end = Offset(thumbX, thumbY + 80.dp.toPx()),
                    strokeWidth = 1.5.dp.toPx()
                )
            }

            // Subtitle
            Text(
                text = "Stability improved by +4%",
                fontSize = 14.sp,
                fontWeight = FontWeight.Normal,
                color = Color.White.copy(alpha = 0.6f),
                modifier = Modifier
                    .align(Alignment.BottomCenter)
                    .padding(bottom = 12.dp),
                textAlign = TextAlign.Center
            )
        }
    }
}

@Composable
fun InsightCard(title: String, description: String, hasPlan: Boolean, onAction: () -> Unit) {
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
                onClick = onAction,
                colors = ButtonDefaults.buttonColors(containerColor = Color.Black),
                shape = CircleShape,
                contentPadding = PaddingValues(horizontal = 24.dp, vertical = 12.dp)
            ) {
                Text(
                    text = if (hasPlan) "View Diet Plan" else "Generate AI Diet Plan",
                    color = Color.White,
                    fontSize = 14.sp
                )
            }
        }
    }
}
