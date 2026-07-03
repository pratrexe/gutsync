package com.example.gutsync.ui.screens

import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.RoundedCornerShape
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
import com.example.gutsync.ui.components.StreakSheetContent
import androidx.lifecycle.viewmodel.compose.viewModel

@Composable
fun TrendsScreen(viewModel: GutSyncViewModel = viewModel()) {
    Column(
        modifier = Modifier
            .fillMaxSize()
    ) {
        StreakSheetContent(
            viewModel = viewModel,
            onDismiss = { /* In-page view, no dismiss needed */ }
        )
    }
}

@Composable
fun TrendBar(label: String, progress: Float) {
    val animatedProgress by animateFloatAsState(targetValue = progress)

    Column(horizontalAlignment = Alignment.CenterHorizontally, verticalArrangement = Arrangement.spacedBy(8.dp)) {
        Box(
            modifier = Modifier
                .width(24.dp)
                .weight(1f)
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
