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
import androidx.compose.ui.layout.ContentScale

import com.example.gutsync.GutSyncViewModel
import com.example.gutsync.data.MicrobeType
import com.example.gutsync.data.MicrobeImpactCalculator
import com.example.gutsync.data.NutrientData
import com.example.gutsync.data.MicrobeShift
import com.example.gutsync.ui.theme.SurfaceContainerHighest
import com.example.gutsync.ui.theme.SurfaceContainerLowest
import com.example.gutsync.ui.theme.TranscityFont

import com.example.gutsync.data.auth.AuthSession

import androidx.compose.foundation.Image
import androidx.compose.material.icons.filled.Person
import coil3.compose.rememberAsyncImagePainter

@Composable
fun DashboardScreen(
    session: AuthSession,
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
        val hasData = currentNutrients.fiber > 0 || currentNutrients.polyphenols > 0 || currentNutrients.sugar > 0 || currentNutrients.saturatedFats > 0
        val finalScore = if (!hasData) 0 else scorecard.gutHealthScore
        finalScore to scorecard.predictedShifts
    }

    LazyColumn(
        modifier = Modifier
            .fillMaxSize()
            .padding(horizontal = 20.dp),
        horizontalAlignment = Alignment.Start,
        verticalArrangement = Arrangement.spacedBy(20.dp)
    ) {
        item {
            val calendar = java.util.Calendar.getInstance()
            val hour = calendar.get(java.util.Calendar.HOUR_OF_DAY)
            val greeting = when (hour) {
                in 0..11 -> "Good morning"
                in 12..16 -> "Good afternoon"
                else -> "Good evening"
            }
            
            Box(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(top = 16.dp),
                contentAlignment = Alignment.CenterStart
            ) {
                Row(
                    modifier = Modifier
                        .clip(CircleShape)
                        .background(Color.Black)
                        .border(BorderStroke(1.dp, Color.White.copy(alpha = 0.05f)), CircleShape)
                        .padding(horizontal = 20.dp, vertical = 10.dp),
                    verticalAlignment = Alignment.Bottom,
                    horizontalArrangement = Arrangement.spacedBy(4.dp)
                ) {
                    Text(
                        text = "$greeting, ",
                        fontSize = 18.sp,
                        color = Color.White.copy(alpha = 0.7f),
                        fontWeight = FontWeight.Normal
                    )
                    Text(
                        text = session.displayName ?: "User",
                        fontSize = 22.sp,
                        color = Color.White,
                        fontWeight = FontWeight.Bold,
                        fontFamily = TranscityFont,
                        modifier = Modifier.offset(y = 2.dp)
                    )
                }

                // Profile Picture (Outside the pill, on the right)
                Box(
                    modifier = Modifier
                        .size(44.dp)
                        .align(Alignment.CenterEnd)
                        .clip(CircleShape)
                        .background(Color.White.copy(alpha = 0.05f))
                        .border(BorderStroke(1.dp, Color.White.copy(alpha = 0.1f)), CircleShape),
                    contentAlignment = Alignment.Center
                ) {
                    if (session.photoUrl != null) {
                        Image(
                            painter = rememberAsyncImagePainter(session.photoUrl),
                            contentDescription = "Profile",
                            modifier = Modifier.fillMaxSize(),
                            contentScale = ContentScale.Crop
                        )
                    } else {
                        Icon(
                            imageVector = Icons.Default.Person,
                            contentDescription = "Profile",
                            tint = Color.White,
                            modifier = Modifier.size(24.dp)
                        )
                    }
                }
            }
        }

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
                            fontFamily = TranscityFont,
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
                        fontFamily = TranscityFont,
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
                icon = Icons.Default.Eco, 
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

    // Random daily phrase based on score
    val healthPhrase = remember(score) {
        val calendar = java.util.Calendar.getInstance()
        val seed = calendar.get(java.util.Calendar.YEAR) * 1000 + calendar.get(java.util.Calendar.DAY_OF_YEAR)
        val random = java.util.Random(seed.toLong())

        val phrases = when {
            score <= 20 -> listOf(
                "Are ya even trying?",
                "Your body has filed a complaint.",
                "This ain't it, chief.",
                "Your organs want a meeting.",
                "Error 404: Healthy habits not found.",
                "Bro is running on vibes.",
                "We need to talk.",
                "Certified gremlin behavior.",
                "Your future self is disappointed.",
                "The stats are... concerning."
            )
            score <= 40 -> listOf(
                "Could be worse. Somehow.",
                "Not exactly a flex.",
                "One salad won't fix this.",
                "We're surviving, not thriving.",
                "Time to lock in.",
                "This is your sign.",
                "Your body deserves better.",
                "Progress starts today.",
                "Room for... lots of improvement.",
                "We can cook. Just not literally."
            )
            score <= 60 -> listOf(
                "Meh. Perfectly average.",
                "You're getting somewhere.",
                "Not bad. Not great.",
                "NPC health stats.",
                "The tutorial isn't over.",
                "Decent... ish.",
                "Could use a little upgrade.",
                "Halfway to greatness.",
                "Mid, but fixable.",
                "You're on the right loading screen."
            )
            score <= 80 -> listOf(
                "Look who's making progress.",
                "Your body approves.",
                "Keep cooking.",
                "That's more like it.",
                "You're doing better than yesterday.",
                "W streak.",
                "Keep the momentum.",
                "Almost elite.",
                "Looking healthier already.",
                "Your future self says thanks."
            )
            else -> listOf(
                "Main character energy.",
                "Built different.",
                "Peak performance unlocked.",
                "Health called. You're hired.",
                "This is what consistency looks like.",
                "Absolute W.",
                "Keep flexing.",
                "You're carrying the leaderboard.",
                "Doctor-approved vibes.",
                "Achievement unlocked: Healthy Human."
            )
        }
        phrases[random.nextInt(phrases.size)]
    }

    Column(
        modifier = Modifier
            .fillMaxWidth()
            .padding(vertical = 16.dp)
    ) {
        // Gut Health Label
        Text(
            text = "Gut Health",
            fontSize = 30.sp,
            fontWeight = FontWeight.Medium,
            fontFamily = TranscityFont,
            color = Color.White.copy(alpha = 0.6f),
            modifier = Modifier.padding(bottom = 4.dp)
        )

        // Score Header
        Row(
            verticalAlignment = Alignment.Bottom,
            modifier = Modifier.padding(bottom = 24.dp)
        ) {
            val scoreColor = if (score >= 80) Color(0xFF52D385) else if (score >= 40) Color(0xFFFFC107) else Color(0xFFFF5252)
            Text(
                text = animatedScore.toString(),
                fontSize = 56.sp,
                fontWeight = FontWeight.Light,
                color = scoreColor
            )
            Text(
                text = " /100",
                fontSize = 24.sp,
                fontWeight = FontWeight.Normal,
                color = Color.White.copy(alpha = 0.4f),
                modifier = Modifier.padding(bottom = 8.dp, start = 4.dp)
            )
        }

        // Arc Gauge Layout
        Box(
            modifier = Modifier
                .fillMaxWidth()
                .height(210.dp) 
                .offset(y = (-60).dp),
            contentAlignment = Alignment.TopCenter
        ) {
            Canvas(modifier = Modifier.fillMaxSize()) {
                val strokeWidth = 28.dp.toPx()
                val padding = strokeWidth / 2 + 16.dp.toPx()
                val arcDiameter = size.width - (padding * 2)
                
                val topLeftOffset = Offset(padding, padding)
                val arcSize = Size(arcDiameter, arcDiameter)
                val radius = arcDiameter / 2
                val center = Offset(padding + radius, padding + radius)

                val startAngle = 150f
                val sweepAngle = 240f
                val currentAngle = startAngle + (sweepAngle * animatedProgress)

                val dotColor = if (score >= 80) Color(0xFF52D385) else if (score >= 40) Color(0xFFFFC107) else Color(0xFFFF5252)
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
                
                // 2. Draw Subtle Ridges Overlay
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

                // 3. Draw Progress Arc
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

                // 5. Left Cap
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
                drawCircle(
                    brush = Brush.radialGradient(
                        colors = listOf(Color.Black.copy(alpha = 0.8f), Color.Transparent),
                        center = Offset(thumbX, thumbY + 8f),
                        radius = strokeWidth
                    ),
                    center = Offset(thumbX, thumbY + 8f),
                    radius = strokeWidth
                )
                drawCircle(
                    color = dotColor,
                    radius = strokeWidth / 2,
                    center = Offset(thumbX, thumbY)
                )
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
            
            // Daily Phrase
            Text(
                text = healthPhrase,
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
