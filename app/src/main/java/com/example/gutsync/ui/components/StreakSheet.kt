package com.example.gutsync.ui.components

import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.core.*
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.animation.slideInVertically
import androidx.compose.animation.slideOutVertically
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Close
import androidx.compose.material.icons.filled.LocalFireDepartment
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.blur
import androidx.compose.ui.draw.clip
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.compose.ui.window.Dialog
import androidx.compose.ui.window.DialogProperties
import com.example.gutsync.GutSyncViewModel
import java.util.*

@Composable
fun StreakSheetContent(
    viewModel: GutSyncViewModel,
    onDismiss: () -> Unit,
    modifier: Modifier = Modifier
) {
    val appData by viewModel.appData.collectAsState()
    val profile = appData.profile

    Box(
        modifier = modifier.fillMaxSize()
    ) {
        StreakAnimatedBackground(
            modifier = Modifier
                .fillMaxSize()
                .blur(30.dp)
        )

        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(24.dp),
            horizontalAlignment = Alignment.CenterHorizontally
        ) {
            Spacer(modifier = Modifier.height(20.dp))

            // Fire Icon & Streak Number
            Box(contentAlignment = Alignment.Center) {
                Box(
                    modifier = Modifier
                        .size(120.dp)
                        .blur(40.dp)
                        .background(Color(0xFFFF5722).copy(alpha = 0.2f), CircleShape)
                )
                Column(horizontalAlignment = Alignment.CenterHorizontally) {
                    Icon(
                        imageVector = Icons.Default.LocalFireDepartment,
                        contentDescription = null,
                        tint = Color(0xFFFF5722),
                        modifier = Modifier.size(60.dp)
                    )
                    Text(
                        text = profile.streakCount.toString(),
                        fontSize = 40.sp,
                        fontWeight = FontWeight.Bold,
                        color = Color.White
                    )
                }
            }

            Text(
                text = "Daily Streaks",
                fontSize = 20.sp,
                fontWeight = FontWeight.Bold,
                color = Color.White,
                modifier = Modifier.padding(top = 16.dp)
            )

            Text(
                text = "You're doing really great!",
                fontSize = 12.sp,
                color = Color.White.copy(alpha = 0.6f),
                modifier = Modifier.padding(top = 4.dp)
            )

            Spacer(modifier = Modifier.height(24.dp))

            // Weekly View
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween
            ) {
                val days = listOf("M", "T", "W", "T", "F", "S", "S")
                val calendar = Calendar.getInstance()
                val currentDayIdx = (calendar.get(Calendar.DAY_OF_WEEK) + 5) % 7

                days.forEachIndexed { index, day ->
                    Column(horizontalAlignment = Alignment.CenterHorizontally) {
                        Text(
                            text = day,
                            fontSize = 10.sp,
                            color = if (index == currentDayIdx) Color.White else Color.White.copy(alpha = 0.4f),
                            fontWeight = if (index == currentDayIdx) FontWeight.Bold else FontWeight.Normal
                        )
                        Spacer(modifier = Modifier.height(6.dp))

                        val isChecked = profile.weeklyCheckIns.getOrNull(index) ?: false
                        val isPast = index < currentDayIdx

                        Box(
                            modifier = Modifier
                                .size(32.dp)
                                .clip(CircleShape)
                                .background(
                                    if (isChecked) Color(0xFFFF5722).copy(alpha = 0.2f)
                                    else Color.White.copy(alpha = 0.05f)
                                )
                                .border(
                                    1.dp,
                                    if (index == currentDayIdx) Color.White.copy(alpha = 0.3f) else Color.Transparent,
                                    CircleShape
                                ),
                            contentAlignment = Alignment.Center
                        ) {
                            if (isChecked) {
                                Icon(
                                    Icons.Default.LocalFireDepartment,
                                    null,
                                    tint = Color(0xFFFF5722),
                                    modifier = Modifier.size(18.dp)
                                )
                            } else if (isPast) {
                                Icon(
                                    Icons.Default.Close,
                                    null,
                                    tint = Color.Red.copy(alpha = 0.6f),
                                    modifier = Modifier.size(16.dp)
                                )
                            }
                        }
                    }
                }
            }

            Spacer(modifier = Modifier.height(32.dp))

            // Streak Freeze Card
            Card(
                colors = CardDefaults.cardColors(containerColor = Color.White.copy(alpha = 0.05f)),
                shape = RoundedCornerShape(20.dp),
                border = androidx.compose.foundation.BorderStroke(1.dp, Color.White.copy(alpha = 0.1f)),
                modifier = Modifier.fillMaxWidth()
            ) {
                Column(
                    modifier = Modifier.padding(16.dp),
                    horizontalAlignment = Alignment.CenterHorizontally
                ) {
                    Text(
                        text = "❄️ Streak Freezes",
                        fontSize = 14.sp,
                        fontWeight = FontWeight.SemiBold,
                        color = Color.White
                    )
                    Spacer(modifier = Modifier.height(8.dp))
                    Text(
                        text = "${profile.streakFreezes} available this week",
                        fontSize = 12.sp,
                        color = Color.White.copy(alpha = 0.6f)
                    )
                    Spacer(modifier = Modifier.height(12.dp))
                    LinearProgressIndicator(
                        progress = { profile.streakFreezes / 2f },
                        modifier = Modifier
                            .fillMaxWidth()
                            .height(6.dp)
                            .clip(CircleShape),
                        color = Color(0xFF03A9F4),
                        trackColor = Color.White.copy(alpha = 0.1f)
                    )
                }
            }

            Spacer(modifier = Modifier.weight(1f))

            // Check-in Button
            val isTodayDone = isCheckInDoneToday(profile.lastCheckInTimestamp)
            Button(
                onClick = {
                    viewModel.performCheckIn()
                    onDismiss()
                },
                modifier = Modifier
                    .fillMaxWidth()
                    .height(56.dp),
                enabled = !isTodayDone,
                colors = ButtonDefaults.buttonColors(
                    containerColor = if (isTodayDone) Color.White.copy(alpha = 0.1f) else Color.White,
                    contentColor = Color.Black
                ),
                shape = RoundedCornerShape(16.dp)
            ) {
                Text(
                    text = if (isTodayDone) "Checked In for Today" else "Complete Today's Check-In",
                    fontSize = 14.sp,
                    fontWeight = FontWeight.Bold
                )
            }
            Spacer(modifier = Modifier.height(20.dp))
        }
    }
}

@Composable
fun StreakSheet(
    viewModel: GutSyncViewModel,
    onDismiss: () -> Unit
) {
    Dialog(
        onDismissRequest = onDismiss,
        properties = DialogProperties(
            usePlatformDefaultWidth = false,
            decorFitsSystemWindows = false
        )
    ) {
        Box(
            modifier = Modifier.fillMaxSize(),
            contentAlignment = Alignment.BottomCenter
        ) {
            Box(
                modifier = Modifier
                    .fillMaxSize()
                    .background(Color.Black.copy(alpha = 0.4f))
                    .clickable { onDismiss() }
            )

            var isVisible by remember { mutableStateOf(false) }
            LaunchedEffect(Unit) { isVisible = true }

            AnimatedVisibility(
                visible = isVisible,
                enter = slideInVertically(
                    initialOffsetY = { it },
                    animationSpec = spring(dampingRatio = Spring.DampingRatioLowBouncy, stiffness = Spring.StiffnessLow)
                ) + fadeIn(),
                exit = slideOutVertically(targetOffsetY = { it }) + fadeOut()
            ) {
                Box(
                    modifier = Modifier
                        .padding(bottom = 32.dp)
                        .widthIn(max = 380.dp)
                        .fillMaxWidth(0.9f)
                        .fillMaxHeight(0.75f)
                        .clip(RoundedCornerShape(40.dp))
                        .background(Color.Black)
                        .border(
                            1.dp,
                            Brush.verticalGradient(
                                listOf(Color.White.copy(alpha = 0.2f), Color.Transparent)
                            ),
                            RoundedCornerShape(40.dp)
                        )
                ) {
                    StreakSheetContent(viewModel, onDismiss)
                }
            }
        }
    }
}

@Composable
fun StreakAnimatedBackground(modifier: Modifier = Modifier) {
    val infiniteTransition = rememberInfiniteTransition(label = "streak_bg")

    val xOffset by infiniteTransition.animateFloat(
        initialValue = 0.2f,
        targetValue = 0.8f,
        animationSpec = infiniteRepeatable(
            animation = tween(8000, easing = LinearEasing),
            repeatMode = RepeatMode.Reverse
        ), label = "x"
    )

    val yOffset by infiniteTransition.animateFloat(
        initialValue = 0.8f,
        targetValue = 0.2f,
        animationSpec = infiniteRepeatable(
            animation = tween(10000, easing = LinearEasing),
            repeatMode = RepeatMode.Reverse
        ), label = "y"
    )

    Canvas(modifier = modifier) {
        val width = size.width
        val height = size.height

        // Transparent background to let the pill's background show through
        drawRect(color = Color.Black.copy(alpha = 0.1f))

        val brush = Brush.radialGradient(
            colors = listOf(
                Color(0xFFFF5722).copy(alpha = 0.2f),
                Color.Transparent
            ),
            center = Offset(width * xOffset, height * yOffset),
            radius = width * 0.9f
        )
        drawRect(brush = brush)

        val brush2 = Brush.radialGradient(
            colors = listOf(
                Color(0xFFE91E63).copy(alpha = 0.15f),
                Color.Transparent
            ),
            center = Offset(width * (1f - xOffset), height * (1f - yOffset)),
            radius = width * 0.7f
        )
        drawRect(brush = brush2)
    }
}

fun isCheckInDoneToday(lastTimestamp: Long): Boolean {
    if (lastTimestamp == 0L) return false
    val now = Calendar.getInstance()
    val last = Calendar.getInstance().apply { timeInMillis = lastTimestamp }
    return now.get(Calendar.YEAR) == last.get(Calendar.YEAR) &&
            now.get(Calendar.DAY_OF_YEAR) == last.get(Calendar.DAY_OF_YEAR)
}
