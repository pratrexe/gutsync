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
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.StrokeCap
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.gutsync.data.MicrobeType
import com.example.gutsync.ui.theme.SurfaceContainerHighest
import com.example.gutsync.ui.theme.SurfaceContainerLowest

@Composable
fun DashboardScreen() {
    LazyColumn(
        modifier = Modifier
            .fillMaxSize()
            .padding(horizontal = 20.dp),
        horizontalAlignment = Alignment.CenterHorizontally,
        verticalArrangement = Arrangement.spacedBy(24.dp)
    ) {
        item { Spacer(modifier = Modifier.height(16.dp)) }
        
        // Score Hero Section
        item {
            ScoreHeroSection(score = 84, growth = 12)
        }

        // Fiber Goal Card
        item {
            GoalCard(label = "Fiber Intake", current = 24, goal = 35)
        }

        // Microbe Status Grid
        item {
            MicrobeStatusGrid()
        }

        // Insight Card
        item {
            InsightCard(
                title = "Focus: Mucosal Integrity",
                description = "Your Akkermansia levels are currently below the target range. Consider adding polyphenol-rich foods like pomegranate or green tea."
            )
        }

        item { Spacer(modifier = Modifier.height(100.dp)) }
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
                        .fillMaxWidth(current.toFloat() / goal)
                        .fillMaxHeight()
                        .background(Color.White, CircleShape)
                )
            }
        }
    }
}

@Composable
fun MicrobeStatusGrid() {
    Column(verticalArrangement = Arrangement.spacedBy(16.dp)) {
        Row(horizontalArrangement = Arrangement.spacedBy(16.dp)) {
            MicrobeStatusCard(MicrobeType.BIFIDOBACTERIUM, 85, "Optimal", Modifier.weight(1f))
            MicrobeStatusCard(MicrobeType.LACTOBACILLUS, 72, "Optimal", Modifier.weight(1f))
        }
        Row(horizontalArrangement = Arrangement.spacedBy(16.dp)) {
            MicrobeStatusCard(MicrobeType.AKKERMANSIA, 15, "Low", Modifier.weight(1f))
            MicrobeStatusCard(MicrobeType.BACTEROIDES, 45, "Moderate", Modifier.weight(1f))
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
                    modifier = Modifier.width(80.dp)
                )
                Surface(
                    color = if (status == "Low") SurfaceContainerHighest else MaterialTheme.colorScheme.surfaceVariant,
                    shape = CircleShape
                ) {
                    Text(
                        text = status,
                        fontSize = 10.sp,
                        color = if (status == "Low") Color.White else MaterialTheme.colorScheme.onSurfaceVariant,
                        modifier = Modifier.padding(horizontal = 8.dp, vertical = 2.dp)
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
