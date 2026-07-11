package com.example.gutsync.ui.screens

import androidx.compose.animation.core.*
import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Eco
import androidx.compose.material.icons.filled.Grain
import androidx.compose.material.icons.filled.Info
import androidx.compose.material.icons.filled.LocalFireDepartment
import androidx.compose.material.icons.filled.Opacity
import androidx.compose.material.icons.filled.Person
import androidx.compose.material.icons.filled.Science
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
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.lifecycle.viewmodel.compose.viewModel
import coil3.compose.rememberAsyncImagePainter
import com.example.gutsync.GutSyncViewModel
import com.example.gutsync.data.MicrobeImpactCalculator
import com.example.gutsync.data.MicrobeShift
import com.example.gutsync.data.NutrientData
import com.example.gutsync.data.auth.AuthSession
import com.example.gutsync.ui.theme.TranscityFont

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

    // Process 7-day Trend Data
    val weeklyTrend = remember(meals) {
        (0 until 7).map { daysAgo ->
            val dayCalendar = java.util.Calendar.getInstance().apply {
                add(java.util.Calendar.DAY_OF_YEAR, -daysAgo)
                set(java.util.Calendar.HOUR_OF_DAY, 0)
                set(java.util.Calendar.MINUTE, 0)
                set(java.util.Calendar.SECOND, 0)
                set(java.util.Calendar.MILLISECOND, 0)
            }
            val nextDayCalendar = (dayCalendar.clone() as java.util.Calendar).apply {
                add(java.util.Calendar.DAY_OF_YEAR, 1)
            }
            
            val daysMeals = meals.filter { 
                it.timestamp >= dayCalendar.timeInMillis && it.timestamp < nextDayCalendar.timeInMillis 
            }
            
            val totalFiber = daysMeals.sumOf { it.nutrients.fiber.toDouble() }.toFloat()
            val totalPolyphenols = daysMeals.sumOf { it.nutrients.polyphenols.toDouble() }.toFloat()
            val totalStarch = daysMeals.sumOf { it.nutrients.resistantStarch.toDouble() }.toFloat()
            
            // Simplified Score (0-1) based on goals
            val score = ((totalFiber / 30f) + (totalPolyphenols / 500f) + (totalStarch / 15f)) / 3f
            score.coerceIn(0.1f, 1f)
        }.reversed()
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
            WeeklyTrendSection(weeklyTrend = weeklyTrend, profile = appData.profile)
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
                        progress = if (appData.profile.resistantStarchGoal > 0) currentNutrients.resistantStarch / appData.profile.resistantStarchGoal else 0f,
                        color = Color(0xFF2196F3),
                        modifier = Modifier.weight(1f)
                    )

                    MetricCard(
                        icon = Icons.Default.Opacity,
                        value = "${currentNutrients.polyphenols.toInt()}",
                        unit = "mg",
                        label = "Polyphenols",
                        subtitle = "Goal: ${appData.profile.polyphenolGoal}mg",
                        progress = if (appData.profile.polyphenolGoal > 0) currentNutrients.polyphenols / appData.profile.polyphenolGoal else 0f,
                        color = Color(0xFF9C27B0),
                        modifier = Modifier.weight(1f)
                    )
                }
            }
        }
        
        item {
            Column(verticalArrangement = Arrangement.spacedBy(16.dp)) {
                Text(
                    text = "Microbe Shifts",
                    fontSize = 24.sp,
                    fontWeight = FontWeight.Bold,
                    fontFamily = TranscityFont,
                    color = Color.White
                )
                MicrobeStatusGrid(shifts = shifts)
            }
        }

        item {
            Column(verticalArrangement = Arrangement.spacedBy(16.dp)) {
                Text(
                    text = "Personalized Insights",
                    fontSize = 24.sp,
                    fontWeight = FontWeight.Bold,
                    fontFamily = TranscityFont,
                    color = Color.White
                )
                
                InsightCard(
                    title = "Microbiome Balance",
                    description = if (healthScore > 70) "Your gut ecosystem is thriving. Bifidobacterium levels are optimal." else "Increase fiber intake to boost SCFA-producing bacteria.",
                    isPositive = healthScore > 70,
                    onClick = {}
                )
                
                InsightCard(
                    title = "Prebiotic Density",
                    description = "Recent meals show strong resistant starch intake, promoting Akkermansia growth.",
                    isPositive = true,
                    onClick = {}
                )
            }
        }
        
        item { Spacer(modifier = Modifier.height(120.dp)) }
    }
}

@Composable
fun WeeklyTrendSection(weeklyTrend: List<Float>, profile: com.example.gutsync.data.storage.UserProfile) {
    Column(
        modifier = Modifier
            .fillMaxWidth()
            .clip(RoundedCornerShape(28.dp))
            .background(Color.White.copy(alpha = 0.03f))
            .border(BorderStroke(1.dp, Color.White.copy(alpha = 0.05f)), RoundedCornerShape(28.dp))
            .padding(20.dp)
    ) {
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically
        ) {
            Text(
                text = "Weekly Trends",
                style = MaterialTheme.typography.titleMedium,
                color = Color.White,
                fontFamily = TranscityFont
            )
            Text(
                text = "Last 7 Days",
                fontSize = 12.sp,
                color = Color.White.copy(alpha = 0.4f)
            )
        }

        Spacer(modifier = Modifier.height(24.dp))

        // 7-Day Bar Chart
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .height(100.dp),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.Bottom
        ) {
            val days = listOf("M", "T", "W", "T", "F", "S", "S")
            val calendar = java.util.Calendar.getInstance()
            val currentDayIdx = (calendar.get(java.util.Calendar.DAY_OF_WEEK) + 5) % 7
            
            weeklyTrend.forEachIndexed { index, score ->
                // Calculate which day of the week this bar represents
                val dayLabel = days[(currentDayIdx - (6 - index) + 7) % 7]
                TrendBar(label = dayLabel, progress = score, isCurrent = index == 6)
            }
        }

        Spacer(modifier = Modifier.height(24.dp))
        HorizontalDivider(color = Color.White.copy(alpha = 0.05f))
        Spacer(modifier = Modifier.height(20.dp))

        // Weekly Check-in Grid
        Text(
            text = "Activity Streak",
            fontSize = 13.sp,
            fontWeight = FontWeight.SemiBold,
            color = Color.White.copy(alpha = 0.6f),
            modifier = Modifier.padding(bottom = 12.dp)
        )

        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.SpaceBetween
        ) {
            val days = listOf("M", "T", "W", "T", "F", "S", "S")
            val calendar = java.util.Calendar.getInstance()
            val currentDayIdx = (calendar.get(java.util.Calendar.DAY_OF_WEEK) + 5) % 7

            days.forEachIndexed { index, day ->
                val isChecked = profile.weeklyCheckIns.getOrNull(index) ?: false
                val isCurrent = index == currentDayIdx

                Box(
                    modifier = Modifier
                        .size(32.dp)
                        .clip(CircleShape)
                        .background(
                            when {
                                isChecked -> Color.White
                                isCurrent -> Color.White.copy(alpha = 0.08f)
                                else -> Color.White.copy(alpha = 0.04f)
                            }
                        )
                        .border(
                            width = if (isCurrent && !isChecked) 1.dp else 0.dp,
                            color = Color.White.copy(alpha = 0.25f),
                            shape = CircleShape
                        ),
                    contentAlignment = Alignment.Center
                ) {
                    if (isChecked) {
                        Icon(
                            Icons.Default.LocalFireDepartment,
                            null,
                            tint = Color.Black,
                            modifier = Modifier.size(16.dp)
                        )
                    } else {
                        Text(
                            text = day,
                            fontSize = 10.sp,
                            color = if (isCurrent) Color.White else Color.White.copy(alpha = 0.25f),
                            fontWeight = if (isCurrent) FontWeight.Bold else FontWeight.Normal
                        )
                    }
                }
            }
        }
    }
}

@Composable
fun TrendBar(label: String, progress: Float, isCurrent: Boolean) {
    val animatedProgress by animateFloatAsState(
        targetValue = progress,
        animationSpec = tween(1000, easing = LinearOutSlowInEasing),
        label = "trend_bar"
    )

    Column(
        horizontalAlignment = Alignment.CenterHorizontally,
        verticalArrangement = Arrangement.spacedBy(8.dp)
    ) {
        Box(
            modifier = Modifier
                .width(28.dp)
                .fillMaxHeight()
                .clip(RoundedCornerShape(8.dp))
                .background(Color.White.copy(alpha = 0.05f)),
            contentAlignment = Alignment.BottomCenter
        ) {
            Box(
                modifier = Modifier
                    .fillMaxWidth()
                    .fillMaxHeight(animatedProgress)
                    .background(if (isCurrent) Color.White else Color.White.copy(alpha = 0.4f))
            )
        }
        Text(
            text = label,
            fontSize = 10.sp,
            color = if (isCurrent) Color.White else Color.White.copy(alpha = 0.3f),
            fontWeight = if (isCurrent) FontWeight.Bold else FontWeight.Medium
        )
    }
}

@Composable
fun MetricCard(
    icon: ImageVector,
    value: String,
    unit: String,
    label: String,
    subtitle: String,
    progress: Float,
    color: Color,
    modifier: Modifier = Modifier,
    score: Float? = null,
    isSlim: Boolean = false
) {
    val animatedProgress by animateFloatAsState(targetValue = progress.coerceIn(0f, 1f), animationSpec = tween(1000), label = "progress")

    Card(
        modifier = modifier,
        shape = RoundedCornerShape(28.dp),
        colors = CardDefaults.cardColors(containerColor = Color.White.copy(alpha = 0.03f)),
        border = BorderStroke(1.dp, Color.White.copy(alpha = 0.05f))
    ) {
        Column(
            modifier = Modifier.padding(20.dp),
            verticalArrangement = Arrangement.spacedBy(12.dp)
        ) {
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.Top
            ) {
                Box(
                    modifier = Modifier
                        .size(40.dp)
                        .clip(CircleShape)
                        .background(color.copy(alpha = 0.1f)),
                    contentAlignment = Alignment.Center
                ) {
                    Icon(icon, null, tint = color, modifier = Modifier.size(20.dp))
                }
                
                if (score != null) {
                    Text(
                        text = "${(score * 100).toInt()}%",
                        color = color,
                        fontWeight = FontWeight.Bold,
                        fontSize = 12.sp
                    )
                }
            }

            Column {
                Row(verticalAlignment = Alignment.Bottom) {
                    Text(
                        text = value,
                        fontSize = 32.sp,
                        fontWeight = FontWeight.Bold,
                        color = Color.White
                    )
                    Text(
                        text = " $unit",
                        fontSize = 16.sp,
                        color = Color.White.copy(alpha = 0.4f),
                        modifier = Modifier.padding(bottom = 6.dp)
                    )
                }
                Text(
                    text = label,
                    fontSize = 14.sp,
                    color = Color.White,
                    fontWeight = FontWeight.Medium
                )
                Text(
                    text = subtitle,
                    fontSize = 12.sp,
                    color = Color.White.copy(alpha = 0.3f)
                )
            }

            // Progress Bar
            Box(
                modifier = Modifier
                    .fillMaxWidth()
                    .height(if (isSlim) 6.dp else 4.dp)
                    .clip(CircleShape)
                    .background(Color.White.copy(alpha = 0.05f))
            ) {
                Box(
                    modifier = Modifier
                        .fillMaxWidth(animatedProgress)
                        .fillMaxHeight()
                        .clip(CircleShape)
                        .background(color)
                )
            }
        }
    }
}

@Composable
fun MicrobeStatusGrid(shifts: List<MicrobeShift>) {
    Column(verticalArrangement = Arrangement.spacedBy(12.dp)) {
        shifts.forEach { shift ->
            Surface(
                color = Color.White.copy(alpha = 0.03f),
                shape = RoundedCornerShape(20.dp),
                border = BorderStroke(1.dp, Color.White.copy(alpha = 0.05f)),
                modifier = Modifier.fillMaxWidth()
            ) {
                Row(
                    modifier = Modifier
                        .padding(16.dp)
                        .fillMaxWidth(),
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.SpaceBetween
                ) {
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        Box(
                            modifier = Modifier
                                .size(32.dp)
                                .clip(CircleShape)
                                .background(Color.White.copy(alpha = 0.05f)),
                            contentAlignment = Alignment.Center
                        ) {
                            Text(
                                text = shift.microbeType.displayName.first().toString(),
                                color = Color.White,
                                fontWeight = FontWeight.Bold,
                                fontSize = 14.sp
                            )
                        }
                        Spacer(modifier = Modifier.width(12.dp))
                        Column {
                            Text(
                                text = shift.microbeType.displayName,
                                color = Color.White,
                                fontSize = 14.sp,
                                fontWeight = FontWeight.Medium
                            )
                            Text(
                                text = getStatusText(shift.shiftPercentage.toInt()),
                                color = Color.White.copy(alpha = 0.4f),
                                fontSize = 11.sp
                            )
                        }
                    }
                    
                    val shiftColor = if (shift.shiftPercentage > 0) Color(0xFF4CAF50) else if (shift.shiftPercentage < 0) Color(0xFFFF5252) else Color.White.copy(alpha = 0.2f)
                    Text(
                        text = if (shift.shiftPercentage > 0) "+${shift.shiftPercentage.toInt()}%" else "${shift.shiftPercentage.toInt()}%",
                        color = shiftColor,
                        fontWeight = FontWeight.Bold,
                        fontSize = 14.sp
                    )
                }
            }
        }
    }
}

fun getStatusText(percentage: Int): String {
    return when {
        percentage > 20 -> "Significant Growth"
        percentage > 0 -> "Stable Growth"
        percentage < -20 -> "Suppressed"
        else -> "Balanced"
    }
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
fun InsightCard(
    title: String,
    description: String,
    isPositive: Boolean,
    onClick: () -> Unit
) {
    Surface(
        onClick = onClick,
        color = Color.White.copy(alpha = 0.03f),
        shape = RoundedCornerShape(24.dp),
        border = BorderStroke(1.dp, Color.White.copy(alpha = 0.05f)),
        modifier = Modifier.fillMaxWidth()
    ) {
        Row(
            modifier = Modifier.padding(16.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            Box(
                modifier = Modifier
                    .size(40.dp)
                    .clip(CircleShape)
                    .background(if (isPositive) Color(0xFF4CAF50).copy(alpha = 0.1f) else Color(0xFFFFC107).copy(alpha = 0.1f)),
                contentAlignment = Alignment.Center
            ) {
                Icon(
                    imageVector = if (isPositive) Icons.Default.Science else Icons.Default.Info,
                    contentDescription = null,
                    tint = if (isPositive) Color(0xFF4CAF50) else Color(0xFFFFC107),
                    modifier = Modifier.size(20.dp)
                )
            }
            
            Spacer(modifier = Modifier.width(16.dp))
            
            Column {
                Text(
                    text = title,
                    color = Color.White,
                    fontSize = 14.sp,
                    fontWeight = FontWeight.Bold
                )
                Text(
                    text = description,
                    color = Color.White.copy(alpha = 0.6f),
                    fontSize = 12.sp,
                    lineHeight = 16.sp
                )
            }
        }
    }
}
