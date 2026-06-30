package com.example.gutsync.ui.theme

import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.darkColorScheme
import androidx.compose.runtime.Composable
import androidx.compose.ui.graphics.Color

private val GutSyncDarkColorScheme = darkColorScheme(
    primary = White,
    secondary = Secondary,
    background = Black,
    surface = Black,
    onPrimary = Black,
    onSecondary = Black,
    onBackground = White,
    onSurface = White,
    surfaceVariant = SurfaceVariant,
    onSurfaceVariant = OnSurfaceVariant,
    outline = Outline,
    error = Error
)

@Composable
fun GutsyncTheme(
    content: @Composable () -> Unit
) {
    MaterialTheme(
        colorScheme = GutSyncDarkColorScheme,
        typography = Typography,
        content = content
    )
}
