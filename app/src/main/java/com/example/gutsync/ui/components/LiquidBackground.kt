package com.example.gutsync.ui.components

import androidx.compose.animation.core.*
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import com.example.gutsync.ui.theme.SurfaceContainerLow

@Composable
fun LiquidBackground() {
    val infiniteTransition = rememberInfiniteTransition(label = "liquid_bg")
    
    // Animate multiple points for a dynamic gradient
    val xOffset1 by infiniteTransition.animateFloat(
        initialValue = 0f,
        targetValue = 1f,
        animationSpec = infiniteRepeatable(
            animation = tween(10000, easing = LinearEasing),
            repeatMode = RepeatMode.Reverse
        ), label = "x1"
    )
    
    val yOffset1 by infiniteTransition.animateFloat(
        initialValue = 0f,
        targetValue = 1f,
        animationSpec = infiniteRepeatable(
            animation = tween(15000, easing = LinearEasing),
            repeatMode = RepeatMode.Reverse
        ), label = "y1"
    )

    val xOffset2 by infiniteTransition.animateFloat(
        initialValue = 1f,
        targetValue = 0f,
        animationSpec = infiniteRepeatable(
            animation = tween(12000, easing = LinearEasing),
            repeatMode = RepeatMode.Reverse
        ), label = "x2"
    )

    Canvas(modifier = Modifier.fillMaxSize()) {
        val width = size.width
        val height = size.height

        // Background base
        drawRect(color = Color.Black)

        // Animated "Liquid" Gradient Blobs
        val brush = Brush.radialGradient(
            colors = listOf(
                Color(0xFF1A1A1A), // Subtle dark grey
                Color.Transparent
            ),
            center = Offset(width * xOffset1, height * yOffset1),
            radius = width * 0.8f
        )
        drawRect(brush = brush)

        val brush2 = Brush.radialGradient(
            colors = listOf(
                Color(0xFF0D0D0D), // Even subtler dark
                Color.Transparent
            ),
            center = Offset(width * xOffset2, height * (1f - yOffset1)),
            radius = width * 1.2f
        )
        drawRect(brush = brush2)
        
        // Add a very subtle purple tint to match the chat accents occasionally
        val brush3 = Brush.radialGradient(
            colors = listOf(
                Color(0xFF6750A4).copy(alpha = 0.05f),
                Color.Transparent
            ),
            center = Offset(width * (1f - xOffset1), height * xOffset2),
            radius = width * 0.5f
        )
        drawRect(brush = brush3)
    }
}
