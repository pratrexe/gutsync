package com.example.gutsync.ui.components

import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.core.*
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.animation.slideInVertically
import androidx.compose.animation.slideOutVertically
import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Close
import androidx.compose.material.icons.filled.EmojiEvents
import androidx.compose.material.icons.filled.LocalFireDepartment
import androidx.compose.material.icons.filled.Lock
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.alpha
import androidx.compose.ui.draw.blur
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.compose.ui.window.Dialog
import androidx.compose.ui.window.DialogProperties
import com.example.gutsync.GutSyncViewModel
import com.example.gutsync.ui.theme.TranscityFont
import java.util.*

@Composable
fun StreakSheetContent(
    viewModel: GutSyncViewModel,
    onDismiss: () -> Unit,
    modifier: Modifier = Modifier
) {
    val appData by viewModel.appData.collectAsState()
    val profile = appData.profile

    Box(modifier = modifier.fillMaxSize()) {

        // Same LiquidBackground as home
        LiquidBackground()

        LazyColumn(
            modifier = Modifier
                .fillMaxSize()
                .padding(horizontal = 28.dp),
            horizontalAlignment = Alignment.CenterHorizontally
        ) {
            item { Spacer(modifier = Modifier.height(56.dp)) }

            // ── Streak number ──────────────────────────────
            item {
                Column(horizontalAlignment = Alignment.CenterHorizontally) {
                    Text(
                        text = profile.streakCount.toString(),
                        fontSize = 96.sp,
                        fontWeight = FontWeight.Black,
                        color = Color.White,
                        letterSpacing = (-4).sp,
                        lineHeight = 96.sp
                    )

                    Text(
                        text = "day streak",
                        fontSize = 16.sp,
                        fontWeight = FontWeight.Medium,
                        color = Color.White.copy(alpha = 0.4f),
                        letterSpacing = 1.sp
                    )
                }

                Spacer(modifier = Modifier.height(48.dp))

                // ── Thin divider ──────────────────────────────
                Box(
                    modifier = Modifier
                        .fillMaxWidth()
                        .height(1.dp)
                        .background(Color.White.copy(alpha = 0.08f))
                )

                Spacer(modifier = Modifier.height(32.dp))
            }

            // ── Weekly grid label ─────────────────────────
            item {
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Text(
                        text = "This week",
                        fontSize = 13.sp,
                        fontWeight = FontWeight.SemiBold,
                        color = Color.White.copy(alpha = 0.6f),
                        letterSpacing = 0.5.sp
                    )
                    Text(
                        text = "Freeze: ${profile.streakFreezes}",
                        fontSize = 12.sp,
                        color = Color.White.copy(alpha = 0.3f)
                    )
                }

                Spacer(modifier = Modifier.height(16.dp))
            }

            // ── Weekly circles ────────────────────────────
            item {
                val days = listOf("M", "T", "W", "T", "F", "S", "S")
                val calendar = Calendar.getInstance()
                val currentDayIdx = (calendar.get(Calendar.DAY_OF_WEEK) + 5) % 7

                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween
                ) {
                    days.forEachIndexed { index, day ->
                        val isChecked = profile.weeklyCheckIns.getOrNull(index) ?: false
                        val isCurrent = index == currentDayIdx
                        val isPast = index < currentDayIdx

                        Column(
                            horizontalAlignment = Alignment.CenterHorizontally,
                            verticalArrangement = Arrangement.spacedBy(8.dp)
                        ) {
                            Text(
                                text = day,
                                fontSize = 10.sp,
                                fontWeight = if (isCurrent) FontWeight.Bold else FontWeight.Normal,
                                color = if (isCurrent) Color.White else Color.White.copy(alpha = 0.25f)
                            )

                            Box(
                                modifier = Modifier
                                    .size(38.dp)
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
                                when {
                                    isChecked -> Icon(
                                        Icons.Default.LocalFireDepartment,
                                        null,
                                        tint = Color.Black,
                                        modifier = Modifier.size(18.dp)
                                    )

                                    isPast -> Icon(
                                        Icons.Default.Close,
                                        null,
                                        tint = Color.White.copy(alpha = 0.2f),
                                        modifier = Modifier.size(14.dp)
                                    )
                                }
                            }
                        }
                    }
                }

                Spacer(modifier = Modifier.height(32.dp))

                // ── Thin divider ──────────────────────────────
                Box(
                    modifier = Modifier
                        .fillMaxWidth()
                        .height(1.dp)
                        .background(Color.White.copy(alpha = 0.08f))
                )

                Spacer(modifier = Modifier.height(32.dp))
            }

            // ── Streak freeze bar ─────────────────────────
            item {
                Column(
                    modifier = Modifier
                        .fillMaxWidth()
                        .clip(RoundedCornerShape(20.dp))
                        .background(Color.White.copy(alpha = 0.04f))
                        .border(
                            1.dp,
                            Brush.verticalGradient(
                                listOf(Color.White.copy(alpha = 0.12f), Color.Transparent)
                            ),
                            RoundedCornerShape(20.dp)
                        )
                        .padding(20.dp)
                ) {
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.SpaceBetween
                    ) {
                        Text(
                            text = "Streak Freezes",
                            fontSize = 13.sp,
                            fontWeight = FontWeight.SemiBold,
                            color = Color.White.copy(alpha = 0.7f)
                        )
                        Text(
                            text = "${profile.streakFreezes} / 2",
                            fontSize = 13.sp,
                            fontWeight = FontWeight.Bold,
                            color = Color.White
                        )
                    }

                    Spacer(modifier = Modifier.height(12.dp))

                    Box(
                        modifier = Modifier
                            .fillMaxWidth()
                            .height(4.dp)
                            .clip(CircleShape)
                            .background(Color.White.copy(alpha = 0.1f))
                    ) {
                        Box(
                            modifier = Modifier
                                .fillMaxWidth(profile.streakFreezes / 2f)
                                .fillMaxHeight()
                                .clip(CircleShape)
                                .background(Color.White)
                        )
                    }

                    Spacer(modifier = Modifier.height(10.dp))

                    Text(
                        text = "Log a meal to protect your streak",
                        fontSize = 11.sp,
                        color = Color.White.copy(alpha = 0.25f)
                    )
                }

                Spacer(modifier = Modifier.height(40.dp))
            }

            // ── Achievements Section ──────────────────────
            item {
                Column(
                    modifier = Modifier.fillMaxWidth(),
                    verticalArrangement = Arrangement.spacedBy(16.dp)
                ) {
                    Text(
                        text = "Achievements",
                        fontSize = 20.sp,
                        fontWeight = FontWeight.Bold,
                        fontFamily = TranscityFont,
                        color = Color.White,
                        modifier = Modifier.align(Alignment.Start)
                    )

                    val achievements = listOf(
                        AchievementData(10, "Novice", "10 Day Streak"),
                        AchievementData(20, "Apprentice", "20 Day Streak"),
                        AchievementData(50, "Warrior", "50 Day Streak"),
                        AchievementData(100, "Master", "100 Day Streak"),
                        AchievementData(200, "Legend", "200 Day Streak"),
                        AchievementData(500, "Godlike", "500 Day Streak")
                    )

                    achievements.chunked(2).forEach { rowItems ->
                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.spacedBy(12.dp)
                        ) {
                            rowItems.forEach { achievement ->
                                AchievementCard(
                                    data = achievement,
                                    isUnlocked = profile.streakCount >= achievement.threshold,
                                    modifier = Modifier.weight(1f)
                                )
                            }
                            if (rowItems.size == 1) {
                                Spacer(modifier = Modifier.weight(1f))
                            }
                        }
                    }
                }
            }

            item { Spacer(modifier = Modifier.height(120.dp)) }
        }
    }
}

data class AchievementData(val threshold: Int, val title: String, val subtitle: String)

@Composable
fun AchievementCard(data: AchievementData, isUnlocked: Boolean, modifier: Modifier = Modifier) {
    val alpha = if (isUnlocked) 1f else 0.2f
    val iconColor = if (isUnlocked) Color(0xFFFFD700) else Color.Gray
    
    Card(
        colors = CardDefaults.cardColors(
            containerColor = if (isUnlocked) Color.White.copy(alpha = 0.08f) else Color.White.copy(alpha = 0.02f)
        ),
        shape = RoundedCornerShape(20.dp),
        border = BorderStroke(1.dp, if (isUnlocked) Color.White.copy(alpha = 0.2f) else Color.White.copy(alpha = 0.05f)),
        modifier = modifier.height(140.dp)
    ) {
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(16.dp),
            horizontalAlignment = Alignment.CenterHorizontally,
            verticalArrangement = Arrangement.Center
        ) {
            Box(contentAlignment = Alignment.Center) {
                Icon(
                    imageVector = if (isUnlocked) Icons.Default.EmojiEvents else Icons.Default.Lock,
                    contentDescription = null,
                    tint = iconColor,
                    modifier = Modifier.size(40.dp).alpha(alpha)
                )
            }
            Spacer(modifier = Modifier.height(12.dp))
            Text(
                text = data.title,
                fontSize = 14.sp,
                fontWeight = FontWeight.Bold,
                color = Color.White.copy(alpha = if (isUnlocked) 1f else 0.4f),
                textAlign = TextAlign.Center
            )
            Text(
                text = data.subtitle,
                fontSize = 11.sp,
                color = Color.White.copy(alpha = if (isUnlocked) 0.6f else 0.2f),
                textAlign = TextAlign.Center
            )
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

fun isCheckInDoneToday(lastTimestamp: Long): Boolean {
    if (lastTimestamp == 0L) return false
    val now = Calendar.getInstance()
    val last = Calendar.getInstance().apply { timeInMillis = lastTimestamp }
    return now.get(Calendar.YEAR) == last.get(Calendar.YEAR) &&
            now.get(Calendar.DAY_OF_YEAR) == last.get(Calendar.DAY_OF_YEAR)
}
